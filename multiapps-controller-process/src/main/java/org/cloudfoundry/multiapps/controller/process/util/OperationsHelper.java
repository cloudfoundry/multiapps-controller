package org.cloudfoundry.multiapps.controller.process.util;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.api.model.ErrorType;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableOperation;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.persistence.model.HistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.process.metadata.ProcessTypeToOperationMetadataMapper;

@Named
public class OperationsHelper {

    private final OperationService operationService;
    private final ProcessTypeToOperationMetadataMapper metadataMapper;
    private final ProcessHelper processHelper;

    @Inject
    public OperationsHelper(OperationService operationService, ProcessTypeToOperationMetadataMapper metadataMapper,
                            ProcessHelper processHelper) {
        this.operationService = operationService;
        this.metadataMapper = metadataMapper;
        this.processHelper = processHelper;

    }

    public String getProcessDefinitionKey(Operation operation) {
        return metadataMapper.getDiagramId(operation.getProcessType());
    }

    public Operation addErrorType(Operation operation) {
        if (operation.getState() == Operation.State.ERROR) {
            return ImmutableOperation.copyOf(operation)
                                     .withErrorType(getErrorType(operation));
        }
        return operation;
    }

    public Operation releaseLockIfNeeded(Operation operation) {
        // Fixes bug XSBUG-2035: Inconsistency in 'operation', 'act_hi_procinst' and 'act_ru_execution' tables
        if (operation.hasAcquiredLock() && operation.getState()
                                                    .isFinal()) {
            operation = ImmutableOperation.builder()
                                          .from(operation)
                                          .hasAcquiredLock(false)
                                          .build();
            operationService.update(operation, operation);
        }
        return operation;
    }

    public List<Operation> releaseLocksIfNeeded(List<Operation> operations) {
        return operations.stream()
                         .map(this::releaseLockIfNeeded)
                         .collect(Collectors.toList());
    }

    private ErrorType getErrorType(Operation operation) {
        List<HistoricOperationEvent> historicEvents = processHelper.getHistoricOperationEventByProcessId(operation.getProcessId());
        if (historicEvents.isEmpty()) {
            return null;
        }
        HistoricOperationEvent.EventType lastEventType = historicEvents.get(historicEvents.size() - 1)
                                                                       .getType();
        return toErrorType(lastEventType);
    }

    private ErrorType toErrorType(HistoricOperationEvent.EventType historicType) {
        if (historicType == HistoricOperationEvent.EventType.FAILED_BY_CONTENT_ERROR) {
            return ErrorType.CONTENT;
        }
        if (historicType == HistoricOperationEvent.EventType.FAILED_BY_INFRASTRUCTURE_ERROR) {
            return ErrorType.INFRASTRUCTURE;
        }
        return null;
    }
}
