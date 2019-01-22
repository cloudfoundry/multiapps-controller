package com.sap.cloud.lm.sl.cf.process.listeners;

import static java.text.MessageFormat.format;

import java.io.Serializable;
import java.text.MessageFormat;
import java.time.ZonedDateTime;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.flowable.common.engine.api.delegate.event.AbstractFlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.HistoryService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.event.FlowableProcessEngineEvent;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.core.helpers.BeanProvider;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.analytics.adapters.FlowableEngineEventToDelegateExecutionAdapter;
import com.sap.cloud.lm.sl.cf.process.analytics.model.AnalyticsData;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ClientReleaser;
import com.sap.cloud.lm.sl.cf.process.util.CollectedDataSender;
import com.sap.cloud.lm.sl.cf.process.util.FileSweeper;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.State;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.Runnable;

@Component("abortProcessListener")
public class AbortProcessListener extends AbstractFlowableEventListener implements Serializable {

    private static final long serialVersionUID = 1L;

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
    @Inject
    private ApplicationConfiguration configuration;
    @Inject
    private CollectedDataSender dataSender;

    @Override
    public boolean isFailOnException() {
        return false;
    }

    @Override
    public void onEvent(FlowableEvent event) {
        if (!isEventValid(event)) {
            return;
        }

        FlowableEngineEvent engineEvent = (FlowableEngineEvent) event;

        String processInstanceId = engineEvent.getProcessInstanceId();
        String correlationId = getCorrelationId(engineEvent);

        new SafeExecutor().executeSafely(() -> setOperationInAbortedState(correlationId));

        HistoryService historyService = Context.getProcessEngineConfiguration()
            .getHistoryService();

        new SafeExecutor().executeSafely(() -> deleteAllocatedRoutes(historyService, processInstanceId));
        new SafeExecutor().executeSafely(() -> deleteDeploymentFiles(historyService, processInstanceId));

        new SafeExecutor().executeSafely(() -> new ClientReleaser(engineEvent, getClientProvider()).releaseClient());

        new SafeExecutor().executeSafely(() -> {
            if (configuration.shouldGatherUsageStatistics()) {
                sendStatistics(engineEvent);
            }
        });

    }

    private String getCorrelationId(FlowableEngineEvent event) {
        HistoricVariableInstance correlationId = getHistoricVarInstanceValue(Context.getProcessEngineConfiguration()
            .getHistoryService(), event.getProcessInstanceId(), Constants.VAR_CORRELATION_ID);
        if (correlationId != null) {
            return (String) correlationId.getValue();
        }
        // The process was started before we introduced subprocesses in our BPMN
        // diagrams. Therefore, the correlation ID is the ID of the
        // process instance.
        return event.getProcessInstanceId();
    }

    protected void setOperationInAbortedState(String processInstanceId) {
        Operation operation = getOperationDao().findRequired(processInstanceId);
        LOGGER.info(MessageFormat.format(Messages.PROCESS_0_RELEASING_LOCK_FOR_MTA_1_IN_SPACE_2, operation.getProcessId(),
            operation.getMtaId(), operation.getSpaceId()));
        operation.setState(State.ABORTED);
        operation.setEndedAt(ZonedDateTime.now());
        operation.setAcquiredLock(false);
        getOperationDao().merge(operation);
        LOGGER.debug(MessageFormat.format(Messages.PROCESS_0_RELEASED_LOCK, operation.getProcessId()));
    }

    protected void deleteAllocatedRoutes(HistoryService historyService, String processInstanceId) {
        HistoricVariableInstance allocatedPortsInstance = getHistoricVarInstanceValue(historyService, processInstanceId,
            Constants.VAR_ALLOCATED_PORTS);
        if (allocatedPortsInstance == null) {
            return;
        }
        CloudControllerClient client = getCloudFoundryClient(historyService, processInstanceId);
        String defaultDomain = client.getDefaultDomain() != null ? client.getDefaultDomain()
            .getName() : null;
        if (defaultDomain == null) {
            LOGGER.warn(Messages.COULD_NOT_COMPUTE_DEFAULT_DOMAIN);
            return;
        }
        Integer[] allocatedPorts = JsonUtil.fromBinaryJson((byte[]) allocatedPortsInstance.getValue(), Integer[].class);
        for (Integer port : allocatedPorts) {
            try {
                client.deleteRoute(port.toString(), defaultDomain);
            } catch (CloudOperationException e) {
                LOGGER.warn(format(Messages.COULD_NOT_DELETE_ROUTE_FOR_PORT, port.toString()));
            }
        }
    }

    protected CloudControllerClient getCloudFoundryClient(HistoryService historyService, String processInstanceId) {
        String user = (String) getHistoricVarInstanceValue(historyService, processInstanceId, Constants.VAR_USER).getValue();
        String organization = (String) getHistoricVarInstanceValue(historyService, processInstanceId, Constants.VAR_ORG).getValue();
        String space = (String) getHistoricVarInstanceValue(historyService, processInstanceId, Constants.VAR_SPACE).getValue();
        return getClientProvider().getControllerClient(user, organization, space, null);
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
            com.sap.cloud.lm.sl.cf.persistence.message.Constants.VARIABLE_NAME_SPACE_ID).getValue();

        FileSweeper fileSweeper = new FileSweeper(spaceId, getBeanProvider().getFileService());
        fileSweeper.sweep(extensionDescriptorFileIds);
        fileSweeper.sweep(appArchiveFileIds);
    }

    protected void sendStatistics(FlowableEngineEvent event) {
        DelegateExecution context = new FlowableEngineEventToDelegateExecutionAdapter(event);
        RestTemplate restTemplate = new RestTemplate();
        AnalyticsData collectedData = dataSender.collectAnalyticsData(context, State.ABORTED);
        dataSender.sendCollectedData(restTemplate, dataSender.convertCollectedAnalyticsDataToXml(context, collectedData));
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

    private CloudControllerClientProvider getClientProvider() {
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

    private boolean isEventValid(FlowableEvent event) {
        if (!(event instanceof FlowableProcessEngineEvent)) {
            return false;
        }
        FlowableProcessEngineEvent processEngineEvent = (FlowableProcessEngineEvent) event;
        if (FlowableEngineEventType.PROCESS_CANCELLED.equals(processEngineEvent.getType())) {
            return true;
        }
        if (FlowableEngineEventType.ENTITY_DELETED.equals(processEngineEvent.getType()) && hasCorrectEntityType(processEngineEvent)) {
            return true;
        }
        return false;
    }

    private boolean hasCorrectEntityType(FlowableProcessEngineEvent processEngineEvent) {
        FlowableEntityEvent flowableEntityEvent = (FlowableEntityEvent) processEngineEvent;
        if (!(flowableEntityEvent.getEntity() instanceof ExecutionEntity)) {
            return false;
        }

        ExecutionEntity executionEntity = (ExecutionEntity) flowableEntityEvent.getEntity();
        return executionEntity.isProcessInstanceType() && Constants.PROCESS_ABORTED.equals(executionEntity.getDeleteReason());
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
