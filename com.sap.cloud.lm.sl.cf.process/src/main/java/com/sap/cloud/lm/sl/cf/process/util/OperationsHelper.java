package com.sap.cloud.lm.sl.cf.process.util;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent;
import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent.EventType;
import com.sap.cloud.lm.sl.cf.core.persistence.service.OperationService;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.metadata.ProcessTypeToOperationMetadataMapper;
import com.sap.cloud.lm.sl.cf.web.api.model.ErrorType;
import com.sap.cloud.lm.sl.cf.web.api.model.ImmutableOperation;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;

@Named
public class OperationsHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationsHelper.class);

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

    private ErrorType getErrorType(Operation operation) {
        List<HistoricOperationEvent> historicEvents = processHelper.getHistoricOperationEventByProcessId(operation.getProcessId());
        if (historicEvents.isEmpty()) {
            return null;
        }
        EventType lastEventType = historicEvents.get(historicEvents.size() - 1)
                                                .getType();
        return toErrorType(lastEventType);
    }

    private ErrorType toErrorType(EventType historicType) {
        if (historicType == EventType.FAILED_BY_CONTENT_ERROR) {
            return ErrorType.CONTENT;
        }
        if (historicType == EventType.FAILED_BY_INFRASTRUCTURE_ERROR) {
            return ErrorType.INFRASTRUCTURE;
        }
        return null;
    }

    public Operation addState(Operation operation) {
        if (operation.getState() != null) {
            return operation;
        }
        Operation.State state = computeState(operation);
        // Fixes bug XSBUG-2035: Inconsistency in 'operation', 'act_hi_procinst' and 'act_ru_execution' tables
        if (operation.hasAcquiredLock() && (state.equals(Operation.State.ABORTED) || state.equals(Operation.State.FINISHED))) {
            operation = ImmutableOperation.builder()
                                          .from(operation)
                                          .hasAcquiredLock(false)
                                          .state(state)
                                          .build();
            operationService.update(operation.getProcessId(), operation);
        }
        return ImmutableOperation.copyOf(operation)
                                 .withState(state);
    }

    public Operation.State computeState(Operation operation) {
        LOGGER.debug(MessageFormat.format(Messages.COMPUTING_STATE_OF_OPERATION, operation.getProcessType(), operation.getProcessId()));
        return computeProcessState(operation.getProcessId());
    }

    public Operation.State computeProcessState(String processId) {
        if (processHelper.isAborted(processId)) {
            return Operation.State.ABORTED;
        }
        if (processHelper.isAtReceiveTask(processId)) {
            return Operation.State.ACTION_REQUIRED;
        }
        if (processHelper.isInErrorState(processId)) {
            return Operation.State.ERROR;
        }
        return processHelper.findProcessInstanceById(processId)
                            .isPresent() ? Operation.State.RUNNING : Operation.State.FINISHED;
    }

    public List<Operation> findOperations(List<Operation> operations, List<Operation.State> statusList) {
        operations = addState(operations);
        return filterBasedOnStates(operations, statusList);
    }

    private List<Operation> addState(List<Operation> operations) {
        return operations.stream()
                         .map(this::addState)
                         .collect(Collectors.toList());
    }

    private List<Operation> filterBasedOnStates(List<Operation> operations, List<Operation.State> statusList) {
        if (CollectionUtils.isEmpty(statusList)) {
            return operations;
        }
        return operations.stream()
                         .filter(operation -> statusList.contains(operation.getState()))
                         .collect(Collectors.toList());
    }

}
