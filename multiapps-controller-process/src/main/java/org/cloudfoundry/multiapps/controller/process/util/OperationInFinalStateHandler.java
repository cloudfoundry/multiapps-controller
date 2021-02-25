package org.cloudfoundry.multiapps.controller.process.util;

import static java.text.MessageFormat.format;

import java.time.ZonedDateTime;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.api.model.ImmutableOperation;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.api.model.Operation.State;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.core.util.LoggingUtil;
import org.cloudfoundry.multiapps.controller.core.util.SafeExecutor;
import org.cloudfoundry.multiapps.controller.persistence.model.HistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableHistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.persistence.services.HistoricOperationEventService;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.dynatrace.DynatraceProcessDuration;
import org.cloudfoundry.multiapps.controller.process.dynatrace.DynatracePublisher;
import org.cloudfoundry.multiapps.controller.process.dynatrace.ImmutableDynatraceProcessDuration;
import org.cloudfoundry.multiapps.controller.process.steps.StepsUtil;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class OperationInFinalStateHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationInFinalStateHandler.class);

    @Inject
    private OperationService operationService;
    @Inject
    private CloudControllerClientProvider clientProvider;
    @Inject
    private FileService fileService;
    @Inject
    private HistoricOperationEventService historicOperationEventService;
    @Inject
    private OperationTimeAggregator operationTimeAggregator;
    @Inject
    private DynatracePublisher dynatracePublisher;
    private final SafeExecutor safeExecutor = new SafeExecutor();

    public void handle(DelegateExecution execution, ProcessType processType, Operation.State state) {
        LoggingUtil.logWithCorrelationId(VariableHandling.get(execution, Variables.CORRELATION_ID), () -> handleInternal(execution, processType, state));
    }

    private void handleInternal(DelegateExecution execution, ProcessType processType, Operation.State state) {
        String correlationId = VariableHandling.get(execution, Variables.CORRELATION_ID);
        safeExecutor.execute(() -> deleteDeploymentFiles(execution));
        safeExecutor.execute(() -> deleteCloudControllerClientForProcess(execution));
        safeExecutor.execute(() -> setOperationState(correlationId, state));
        safeExecutor.execute(() -> trackOperationDuration(correlationId, execution, processType, state));
    }

    protected void deleteDeploymentFiles(DelegateExecution execution) throws FileStorageException {
        if (VariableHandling.get(execution, Variables.KEEP_FILES)) {
            return;
        }

        String extensionDescriptorFileIds = VariableHandling.get(execution, Variables.EXT_DESCRIPTOR_FILE_ID);
        String appArchiveFileIds = VariableHandling.get(execution, Variables.APP_ARCHIVE_ID);

        FileSweeper fileSweeper = new FileSweeper(VariableHandling.get(execution, Variables.SPACE_GUID), fileService);
        fileSweeper.sweep(extensionDescriptorFileIds);
        fileSweeper.sweep(appArchiveFileIds);
    }

    private void deleteCloudControllerClientForProcess(DelegateExecution execution) {
        String user = StepsUtil.determineCurrentUser(execution);
        String organizationName = VariableHandling.get(execution, Variables.ORGANIZATION_NAME);
        String spaceName = VariableHandling.get(execution, Variables.SPACE_NAME);
        String spaceGuid = VariableHandling.get(execution, Variables.SPACE_GUID);

        clientProvider.releaseClient(user, organizationName, spaceName);
        clientProvider.releaseClient(user, spaceGuid);
    }

    protected void setOperationState(String processInstanceId, Operation.State state) {
        Operation operation = operationService.createQuery()
                                              .processId(processInstanceId)
                                              .singleResult();
        if (isOperationAlreadyFinal(operation)) {
            return;
        }
        LOGGER.info(format(Messages.PROCESS_0_RELEASING_LOCK_FOR_MTA_1_IN_SPACE_2, operation.getProcessId(), operation.getMtaId(),
                           operation.getSpaceId()));
        operation = ImmutableOperation.builder()
                                      .from(operation)
                                      .state(state)
                                      .hasAcquiredLock(false)
                                      .endedAt(ZonedDateTime.now())
                                      .build();
        operationService.update(operation, operation);
        LOGGER.debug(format(Messages.PROCESS_0_RELEASED_LOCK, operation.getProcessId()));
        historicOperationEventService.add(ImmutableHistoricOperationEvent.of(processInstanceId, toEventType(state)));
    }

    private boolean isOperationAlreadyFinal(Operation operation) {
        return operation.getState()
                        .isFinal() && !operation.hasAcquiredLock() && operation.getEndedAt() != null;
    }

    private HistoricOperationEvent.EventType toEventType(State state) {
        return state == Operation.State.FINISHED ? HistoricOperationEvent.EventType.FINISHED : HistoricOperationEvent.EventType.ABORTED;
    }

    private void trackOperationDuration(String correlationId, DelegateExecution execution, ProcessType processType, Operation.State state) {
        Map<String, ProcessTime> processTimes = operationTimeAggregator.collectProcessTimes(correlationId);

        processTimes.forEach((processId, processTime) -> logProcessTime(correlationId, processId, processTime));

        ProcessTime overallProcessTime = operationTimeAggregator.computeOverallProcessTime(correlationId, processTimes);
        
        DynatraceProcessDuration dynatraceProcessDuration = ImmutableDynatraceProcessDuration.builder()
                                                                                             .processId(correlationId)
                                                                                             .mtaId(VariableHandling.get(execution, Variables.MTA_ID))
                                                                                             .spaceId(VariableHandling.get(execution, Variables.SPACE_GUID))
                                                                                             .operationState(state)
                                                                                             .processType(processType)
                                                                                             .processDuration(overallProcessTime.getProcessDuration())
                                                                                             .build();
       dynatracePublisher.publishProcessDuration(dynatraceProcessDuration, LOGGER);
        
        LOGGER.info(format(Messages.TIME_STATISTICS_FOR_OPERATION_0_DURATION_1_DELAY_2, correlationId,
                           overallProcessTime.getProcessDuration(), overallProcessTime.getDelayBetweenSteps()));
    }

    private void logProcessTime(String correlationId, String processId, ProcessTime processTime) {
        LOGGER.debug(format(Messages.TIME_STATISTICS_FOR_PROCESS_0_OPERATION_1_DURATION_2_DELAY_3, processId, correlationId,
                            processTime.getProcessDuration(), processTime.getDelayBetweenSteps()));
    }
    
}
