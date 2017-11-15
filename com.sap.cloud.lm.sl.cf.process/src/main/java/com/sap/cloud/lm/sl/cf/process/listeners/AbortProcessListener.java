package com.sap.cloud.lm.sl.cf.process.listeners;

import static java.text.MessageFormat.format;

import java.io.Serializable;
import java.time.ZonedDateTime;

import javax.inject.Inject;

import org.activiti.engine.HistoryService;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.impl.event.logger.handler.ProcessInstanceEndedEventHandler;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.util.GsonHelper;
import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.core.helpers.BeanProvider;
import com.sap.cloud.lm.sl.cf.core.util.Configuration;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.analytics.ActivitiEventToDelegateExecutionAdapter;
import com.sap.cloud.lm.sl.cf.process.analytics.AnalyticsCollector;
import com.sap.cloud.lm.sl.cf.process.analytics.model.AnalyticsData;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ClientReleaser;
import com.sap.cloud.lm.sl.cf.process.util.FileSweeper;
import com.sap.cloud.lm.sl.cf.process.util.ProcessConflictPreventer;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.State;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.Runnable;
import com.sap.cloud.lm.sl.persistence.services.FileStorageException;

@Component("abortProcessListener")
public class AbortProcessListener implements ActivitiEventListener, Serializable {

    private static final long serialVersionUID = -7665948468083310385L;

    private static final Logger LOGGER = LoggerFactory.getLogger(AbortProcessListener.class);

    @Inject
    private AnalyticsCollector analytics;

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
    @Inject
    private Configuration configuration;

    @Override
    public boolean isFailOnException() {
        return false;
    }

    @Override
    public void onEvent(ActivitiEvent event) {
        if (!isEventValid(event)) {
            return;
        }

        String processInstanceId = event.getProcessInstanceId();
        String correlationId = getCorrelationId(event);

        new SafeExecutor().executeSafely(() -> {
            new ProcessConflictPreventer(getOperationDao()).attemptToReleaseLock(correlationId);
        });

        new SafeExecutor().executeSafely(() -> {
            setOperationInAbortedState(correlationId);
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

        new SafeExecutor().executeSafely(() -> {
            if (configuration.shouldGatherUsageStatistics()) {
                collectAnalytics(event);
            }
            // TODO send generated statistics to statistics server.
        });

    }

    private AnalyticsData collectAnalytics(ActivitiEvent event) throws SLException {
        AnalyticsData model = analytics.collectAttributes(new ActivitiEventToDelegateExecutionAdapter(event));
        model.setProcessFinalState(State.ABORTED);
        LOGGER.debug(JsonUtil.toJson(model, true));
        return model;
    }

    private String getCorrelationId(ActivitiEvent event) {
        HistoricVariableInstance correlationId = getHistoricVarInstanceValue(event.getEngineServices().getHistoryService(),
            event.getProcessInstanceId(), Constants.VAR_CORRELATION_ID);
        if (correlationId != null) {
            return (String) correlationId.getValue();
        }
        // The process was started before we introduced subprocesses in our BPMN diagrams. Therefore, the correlation ID is the ID of the
        // process instance.
        return event.getProcessInstanceId();
    }

    protected void setOperationInAbortedState(String processInstanceId) throws NotFoundException {
        Operation operation = getOperationDao().findRequired(processInstanceId);
        operation.setState(State.ABORTED);
        operation.setEndedAt(ZonedDateTime.now());
        getOperationDao().merge(operation);
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
            com.sap.cloud.lm.sl.persistence.message.Constants.VARIABLE_NAME_SPACE_ID).getValue();

        FileSweeper fileSweeper = new FileSweeper(spaceId, getBeanProvider().getFileService());
        fileSweeper.sweep(extensionDescriptorFileIds);
        fileSweeper.sweep(appArchiveFileIds);
    }

    private boolean shouldKeepFiles(HistoricVariableInstance keepFiles) {
        return keepFiles != null && Boolean.TRUE.equals(keepFiles.getValue());
    }

    protected HistoricVariableInstance getHistoricVarInstanceValue(HistoryService historyService, String processInstanceId,
        String parameter) {
        return historyService.createHistoricVariableInstanceQuery()
            .processInstanceId(processInstanceId)
            .variableName(parameter)
            .singleResult();
    }

    private CloudFoundryClientProvider getClientProvider() {
        return getBeanProvider().getCloudFoundryClientProvider();
    }

    private OperationDao getOperationDao() {
        return getBeanProvider().getOperationDao();
    }

    private BeanProvider getBeanProvider() {
        if (beanProvider == null) {
            beanProvider = BeanProvider.getInstance();
        }
        return beanProvider;
    }

    /*
     * This is a workaround for a bug in the activiti engine. If the event listener is configured for ENTITY_DELETED event on entity type
     * "process-instance", the event listener is also triggered for entity type "execution". In activiti engine version 5.16.0 the timer
     * behavior was changed to start an Execution process, which, after the execution of the timer, is deleted. This leads to the triggering
     * of this event listener. The workaround implemented is to check whether there is a delete reason specified, as the deletion for the
     * Execution process for the timer doesn't specify a reason.
     */
    private boolean isEventValid(ActivitiEvent event) {
        ProcessInstanceEndedEventHandler eventHandler = new ProcessInstanceEndedEventHandler();
        eventHandler.setEvent(event);
        ExecutionEntity entity = eventHandler.getEntityFromEvent();
        return entity.getDeleteReason() != null;
    }

    private class SafeExecutor {

        private void executeSafely(Runnable runnable) {
            try {
                runnable.run();
            } catch (Exception e) { // NOSONAR
                LOGGER.warn(e.getMessage(), e);
            }
        }

    }

}
