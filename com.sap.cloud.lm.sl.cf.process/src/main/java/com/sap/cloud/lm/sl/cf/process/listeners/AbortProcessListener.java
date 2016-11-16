package com.sap.cloud.lm.sl.cf.process.listeners;

import static java.text.MessageFormat.format;

import java.io.Serializable;

import javax.inject.Inject;

import org.activiti.engine.HistoryService;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.history.HistoricVariableInstance;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.util.GsonHelper;
import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.cf.core.dao.OngoingOperationDao;
import com.sap.cloud.lm.sl.cf.core.helpers.BeanProvider;
import com.sap.cloud.lm.sl.cf.core.model.OngoingOperation;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ClientReleaser;
import com.sap.cloud.lm.sl.cf.process.util.FileSweeper;
import com.sap.cloud.lm.sl.cf.process.util.ProcessConflictPreventer;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.Runnable;
import com.sap.cloud.lm.sl.persistence.services.FileStorageException;
import com.sap.lmsl.slp.SlpTaskState;

@Component("abortProcessListener")
public class AbortProcessListener implements ActivitiEventListener, Serializable {

    private static final long serialVersionUID = -7665948468083310385L;

    private static final Logger LOGGER = LoggerFactory.getLogger(AbortProcessListener.class);

    /*
     * In older version of the Activiti process diagram, the AbortProcessListener was defined with its fully qualified class name. In the
     * new versions of the Activiti process diagram it is defined with the delegateExpression and the Activiti will get the instance of the
     * AbortProcessListener from the Spring configuration. When using the AbortProcessListener(aborting process or a process fails) with an
     * old Activiti process diagram, in which the AbortProcessListener is defined by specifying the fully qualified class name, and attempt
     * to use the AbortProcessListener(attempt to abort a process) the Activiti tries to initialize the AbortProcessListener by calling its
     * constructor. This happens because the Activiti saved the version of the process diagram with which the process was aborted. And when
     * such abort is being executed the Activiti initializes the AbortProcessListener with its constructor and all the @Inject resources in
     * it are automatically null.
     */
    @Inject
    private BeanProvider beanProvider;

    @Override
    public boolean isFailOnException() {
        return false;
    }

    @Override
    public void onEvent(ActivitiEvent event) {
        String processInstanceId = event.getProcessInstanceId();

        new SafeExecutor().executeSafely(() -> {
            new ProcessConflictPreventer(getOngoingOperationDao()).attemptToReleaseLock(processInstanceId);
        });

        new SafeExecutor().executeSafely(() -> {
            setOngoingOperationInAbortedState(processInstanceId);
        });

        HistoryService historyService = event.getEngineServices().getHistoryService();

        new SafeExecutor().executeSafely(() -> {
            deleteAllocatedRoutes(historyService, processInstanceId);
        });
        new SafeExecutor().executeSafely(() -> {
            deleteDeploymentFiles(historyService, processInstanceId);
        });

        new SafeExecutor().executeSafely(() -> {
            new ClientReleaser(event, getClientProvider()).releaseClient();
        });
    }

    protected void setOngoingOperationInAbortedState(String processInstanceId) throws NotFoundException {
        OngoingOperation ongoingOperation = getOngoingOperationDao().find(processInstanceId);
        ongoingOperation.setFinalState(SlpTaskState.SLP_TASK_STATE_ABORTED);
        getOngoingOperationDao().merge(ongoingOperation);
    }

    protected void deleteAllocatedRoutes(HistoryService historyService, String processInstanceId) throws SLException {
        HistoricVariableInstance allocatedPortsInstance = getHistoricVarInstanceValue(historyService, processInstanceId,
            Constants.VAR_ALLOCATED_PORTS);
        if (allocatedPortsInstance == null) {
            return;
        }
        CloudFoundryOperations client = getCloudFoundryClient(historyService, processInstanceId);
        String defaultDomain = client.getDefaultDomain() != null ? client.getDefaultDomain().getName() : null;
        if (defaultDomain == null) {
            LOGGER.warn(Messages.COULD_NOT_COMPUTE_DEFAULT_DOMAIN);
            return;
        }
        Integer[] allocatedPorts = GsonHelper.getFromBinaryJson((byte[]) allocatedPortsInstance.getValue(), Integer[].class);
        for (Integer port : allocatedPorts) {
            try {
                client.deleteRoute(port.toString(), defaultDomain);
            } catch (CloudFoundryException e) {
                LOGGER.warn(format(Messages.COULD_NOT_DELETE_ROUTE_FOR_PORT, port));
            }
        }
    }

    protected CloudFoundryOperations getCloudFoundryClient(HistoryService historyService, String processInstanceId) throws SLException {
        String user = (String) getHistoricVarInstanceValue(historyService, processInstanceId, Constants.VAR_USER).getValue();
        String organization = (String) getHistoricVarInstanceValue(historyService, processInstanceId, Constants.VAR_ORG).getValue();
        String space = (String) getHistoricVarInstanceValue(historyService, processInstanceId, Constants.VAR_SPACE).getValue();
        return getClientProvider().getCloudFoundryClient(user, organization, space, null);
    }

    protected void deleteDeploymentFiles(HistoryService historyService, String processInstanceId) throws FileStorageException {
        HistoricVariableInstance keepFiles = getHistoricVarInstanceValue(historyService, processInstanceId, Constants.PARAM_KEEP_FILES);
        if (shouldKeepFiles(keepFiles)) {
            return;
        }
        HistoricVariableInstance extensionDescriptorFileIds = getHistoricVarInstanceValue(historyService, processInstanceId,
            Constants.PARAM_EXT_DESCRIPTOR_FILE_ID);
        HistoricVariableInstance appArchiveFileIds = getHistoricVarInstanceValue(historyService, processInstanceId,
            Constants.PARAM_APP_ARCHIVE_ID);

        String spaceId = (String) getHistoricVarInstanceValue(historyService, processInstanceId,
            com.sap.cloud.lm.sl.slp.Constants.VARIABLE_NAME_SPACE_ID).getValue();

        FileSweeper fileSweeper = new FileSweeper(spaceId, getBeanProvider().getFileService());
        fileSweeper.sweep(extensionDescriptorFileIds);
        fileSweeper.sweep(appArchiveFileIds);
    }

    private boolean shouldKeepFiles(HistoricVariableInstance keepFiles) {
        return keepFiles != null && Boolean.TRUE.equals(keepFiles.getValue());
    }

    protected HistoricVariableInstance getHistoricVarInstanceValue(HistoryService historyService, String processInstanceId,
        String parameter) {
        return historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceId).variableName(
            parameter).singleResult();
    }

    private CloudFoundryClientProvider getClientProvider() {
        return getBeanProvider().getCloudFoundryClientProvider();
    }

    private OngoingOperationDao getOngoingOperationDao() {
        return getBeanProvider().getOngoingOperationDao();
    }

    private BeanProvider getBeanProvider() {
        if (beanProvider == null) {
            beanProvider = BeanProvider.getInstance();
        }
        return beanProvider;
    }

    private class SafeExecutor {

        private void executeSafely(Runnable runnable) {
            try {
                runnable.run();
            } catch (Exception e) { // NOSONAR
                LOGGER.warn(Messages.ERROR_OCCURRED_DURING_PROCESS_ABORT, e);
            }
        }

    }

}
