package com.sap.cloud.lm.sl.cf.process.util;

import java.time.ZonedDateTime;

import javax.inject.Inject;
import javax.inject.Named;

import org.flowable.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent.EventType;
import com.sap.cloud.lm.sl.cf.core.persistence.service.OperationService;
import com.sap.cloud.lm.sl.cf.core.util.LoggingUtil;
import com.sap.cloud.lm.sl.cf.core.util.SafeExecutor;
import com.sap.cloud.lm.sl.cf.persistence.services.FileService;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.cf.process.variables.VariableHandling;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.cf.web.api.model.ImmutableOperation;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation.State;

@Named
public class OperationInFinalStateHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationInFinalStateHandler.class);

    public static Logger getLogger() {
        return LOGGER;
    }

    @Inject
    private OperationService operationService;
    @Inject
    private CloudControllerClientProvider clientProvider;
    @Inject
    private FileService fileService;
    @Inject
    private HistoricOperationEventPersister historicOperationEventPersister;
    @Inject
    private OperationTimeAggregator operationTimeAggregator;
    private final SafeExecutor safeExecutor = new SafeExecutor();

    public void handle(DelegateExecution execution, Operation.State state) {
        LoggingUtil.logWithCorrelationId(VariableHandling.get(execution, Variables.CORRELATION_ID), () -> handleInternal(execution, state));
    }

    private void handleInternal(DelegateExecution execution, Operation.State state) {
        String correlationId = VariableHandling.get(execution, Variables.CORRELATION_ID);
        safeExecutor.execute(() -> deleteDeploymentFiles(execution));
        safeExecutor.execute(() -> deleteCloudControllerClientForProcess(execution));
        safeExecutor.execute(() -> releaseOperationLock(correlationId));
        safeExecutor.execute(() -> setOperationState(correlationId, state));
        safeExecutor.execute(() -> operationTimeAggregator.aggregateOperationTime(correlationId));
    }

    protected void deleteDeploymentFiles(DelegateExecution execution) throws FileStorageException {
        if (VariableHandling.get(execution, Variables.KEEP_FILES)) {
            return;
        }

        String extensionDescriptorFileIds = VariableHandling.get(execution, Variables.EXT_DESCRIPTOR_FILE_ID);
        String appArchiveFileIds = VariableHandling.get(execution, Variables.APP_ARCHIVE_ID);

        FileSweeper fileSweeper = new FileSweeper(VariableHandling.get(execution, Variables.SPACE_ID), fileService);
        fileSweeper.sweep(extensionDescriptorFileIds);
        fileSweeper.sweep(appArchiveFileIds);
    }

    private void deleteCloudControllerClientForProcess(DelegateExecution execution) {
        String user = StepsUtil.determineCurrentUser(execution);
        String space = VariableHandling.get(execution, Variables.SPACE);
        String org = VariableHandling.get(execution, Variables.ORG);
        String spaceId = VariableHandling.get(execution, Variables.SPACE_ID);

        clientProvider.releaseClient(user, org, space);
        clientProvider.releaseClient(user, spaceId);
    }

    private void releaseOperationLock(String processInstanceId) {
        ProcessConflictPreventer processConflictPreventer = new ProcessConflictPreventer(operationService);
        processConflictPreventer.releaseLock(processInstanceId);
    }

    protected void setOperationState(String processInstanceId, Operation.State state) {
        Operation operation = operationService.createQuery()
                                              .processId(processInstanceId)
                                              .singleResult();
        operation = ImmutableOperation.builder()
                                      .from(operation)
                                      .state(state)
                                      .endedAt(ZonedDateTime.now())
                                      .build();
        operationService.update(operation.getProcessId(), operation);
        historicOperationEventPersister.add(processInstanceId, toEventType(state));
    }

    private EventType toEventType(State state) {
        return state == Operation.State.FINISHED ? EventType.FINISHED : EventType.ABORTED;
    }

}
