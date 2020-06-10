package org.cloudfoundry.multiapps.controller.process.util;

import java.text.MessageFormat;
import java.time.ZonedDateTime;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.api.model.ImmutableOperation;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.api.model.Operation.State;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.core.model.HistoricOperationEvent.EventType;
import org.cloudfoundry.multiapps.controller.core.persistence.service.OperationService;
import org.cloudfoundry.multiapps.controller.core.util.LoggingUtil;
import org.cloudfoundry.multiapps.controller.core.util.SafeExecutor;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.process.Messages;
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
        safeExecutor.execute(() -> setOperationState(correlationId, state));
        safeExecutor.execute(() -> operationTimeAggregator.aggregateOperationTime(execution, state));
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
        LOGGER.info(MessageFormat.format(Messages.PROCESS_0_RELEASING_LOCK_FOR_MTA_1_IN_SPACE_2, operation.getProcessId(),
                                         operation.getMtaId(), operation.getSpaceId()));
        operation = ImmutableOperation.builder()
                                      .from(operation)
                                      .state(state)
                                      .hasAcquiredLock(false)
                                      .endedAt(ZonedDateTime.now())
                                      .build();
        operationService.update(operation, operation);
        LOGGER.debug(MessageFormat.format(Messages.PROCESS_0_RELEASED_LOCK, operation.getProcessId()));
        historicOperationEventPersister.add(processInstanceId, toEventType(state));
    }

    private EventType toEventType(State state) {
        return state == Operation.State.FINISHED ? EventType.FINISHED : EventType.ABORTED;
    }

}
