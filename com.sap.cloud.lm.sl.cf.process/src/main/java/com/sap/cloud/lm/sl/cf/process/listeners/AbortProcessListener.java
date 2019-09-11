package com.sap.cloud.lm.sl.cf.process.listeners;

import java.io.Serializable;
import java.text.MessageFormat;
import java.time.ZonedDateTime;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.util.RestUtil;
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
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent.EventType;
import com.sap.cloud.lm.sl.cf.core.persistence.service.OperationService;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.persistence.services.FileService;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.analytics.adapters.FlowableEngineEventToDelegateExecutionAdapter;
import com.sap.cloud.lm.sl.cf.process.analytics.model.AnalyticsData;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ClientReleaser;
import com.sap.cloud.lm.sl.cf.process.util.CollectedDataSender;
import com.sap.cloud.lm.sl.cf.process.util.FileSweeper;
import com.sap.cloud.lm.sl.cf.process.util.HistoricOperationEventPersister;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.State;
import com.sap.cloud.lm.sl.common.util.Runnable;

@Named("abortProcessListener")
public class AbortProcessListener extends AbstractFlowableEventListener implements Serializable {

    private static final long serialVersionUID = 2L;

    private static final Logger LOGGER = LoggerFactory.getLogger(AbortProcessListener.class);

    @Inject
    private OperationService operationService;
    @Inject
    private CloudControllerClientProvider clientProvider;
    @Inject
    private FileService fileService;
    @Inject
    private ApplicationConfiguration configuration;
    @Inject
    private CollectedDataSender dataSender;
    @Inject
    private HistoricOperationEventPersister historicOperationEventPersister;

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

        new SafeExecutor().executeSafely(() -> historicOperationEventPersister.add(correlationId, EventType.ABORTED));

        HistoryService historyService = Context.getProcessEngineConfiguration()
                                               .getHistoryService();

        new SafeExecutor().executeSafely(() -> deleteDeploymentFiles(historyService, processInstanceId));

        new SafeExecutor().executeSafely(() -> new ClientReleaser(clientProvider).releaseClientFor(historyService,
                                                                                                   engineEvent.getProcessInstanceId()));

        new SafeExecutor().executeSafely(() -> {
            if (configuration.shouldGatherUsageStatistics()) {
                sendStatistics(engineEvent);
            }
        });

    }

    private String getCorrelationId(FlowableEngineEvent event) {
        HistoricVariableInstance correlationId = getHistoricVarInstanceValue(Context.getProcessEngineConfiguration()
                                                                                    .getHistoryService(),
                                                                             event.getProcessInstanceId(), Constants.VAR_CORRELATION_ID);
        if (correlationId != null) {
            return (String) correlationId.getValue();
        }
        // The process was started before we introduced subprocesses in our BPMN
        // diagrams. Therefore, the correlation ID is the ID of the
        // process instance.
        return event.getProcessInstanceId();
    }

    protected void setOperationInAbortedState(String processInstanceId) {
        Operation operation = operationService.createQuery()
                                              .processId(processInstanceId)
                                              .singleResult();
        LOGGER.info(MessageFormat.format(Messages.PROCESS_0_RELEASING_LOCK_FOR_MTA_1_IN_SPACE_2, operation.getProcessId(),
                                         operation.getMtaId(), operation.getSpaceId()));
        operation.setState(State.ABORTED);
        operation.setEndedAt(ZonedDateTime.now());
        operation.setAcquiredLock(false);
        operationService.update(operation.getProcessId(), operation);
        LOGGER.debug(MessageFormat.format(Messages.PROCESS_0_RELEASED_LOCK, operation.getProcessId()));
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
                                                              com.sap.cloud.lm.sl.cf.persistence.Constants.VARIABLE_NAME_SPACE_ID).getValue();

        FileSweeper fileSweeper = new FileSweeper(spaceId, fileService);
        fileSweeper.sweep(extensionDescriptorFileIds);
        fileSweeper.sweep(appArchiveFileIds);
    }

    protected void sendStatistics(FlowableEngineEvent event) {
        DelegateExecution context = new FlowableEngineEventToDelegateExecutionAdapter(event);
        RestTemplate restTemplate = new RestUtil().createRestTemplate(null, false);
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

    private boolean isEventValid(FlowableEvent event) {
        if (!(event instanceof FlowableProcessEngineEvent)) {
            return false;
        }
        FlowableProcessEngineEvent processEngineEvent = (FlowableProcessEngineEvent) event;
        if (FlowableEngineEventType.PROCESS_CANCELLED.equals(processEngineEvent.getType())) {
            return true;
        }
        return FlowableEngineEventType.ENTITY_DELETED.equals(processEngineEvent.getType()) && hasCorrectEntityType(processEngineEvent);
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
