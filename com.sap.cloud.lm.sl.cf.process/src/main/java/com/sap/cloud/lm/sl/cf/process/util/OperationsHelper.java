package com.sap.cloud.lm.sl.cf.process.util;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.collections4.CollectionUtils;
import org.flowable.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent;
import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent.EventType;
import com.sap.cloud.lm.sl.cf.core.persistence.service.HistoricOperationEventService;
import com.sap.cloud.lm.sl.cf.core.persistence.service.OperationService;
import com.sap.cloud.lm.sl.cf.process.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.metadata.ProcessTypeToOperationMetadataMapper;
import com.sap.cloud.lm.sl.cf.web.api.model.ErrorType;
import com.sap.cloud.lm.sl.cf.web.api.model.ImmutableOperation;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;

@Named
public class OperationsHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationsHelper.class);

    @Inject
    private OperationService operationService;
    @Inject
    private ProcessTypeToOperationMetadataMapper metadataMapper;
    @Inject
    private FlowableFacade flowableFacade;
    @Inject
    private HistoricOperationEventService historicOperationEventService;

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
        List<HistoricOperationEvent> historicEvents = historicOperationEventService.createQuery()
                                                                                   .processId(operation.getProcessId())
                                                                                   .list();
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
            this.operationService.update(operation.getProcessId(), operation);
        }
        return ImmutableOperation.copyOf(operation)
                                 .withState(state);
    }

    public Operation.State computeState(Operation operation) {
        LOGGER.debug(MessageFormat.format(Messages.COMPUTING_STATE_OF_OPERATION, operation.getProcessType(), operation.getProcessId()));
        return computeState(operation.getProcessId());
    }

    public Operation.State computeState(String processId) {
        ProcessInstance processInstance = flowableFacade.getProcessInstance(processId);
        if (processInstance != null) {
            return computeNonFinalState(processInstance);
        }
        return computeFinalState(processId);
    }

    private Operation.State computeNonFinalState(ProcessInstance processInstance) {
        String processInstanceId = processInstance.getProcessInstanceId();
        if (flowableFacade.isProcessInstanceAtReceiveTask(processInstanceId)) {
            return Operation.State.ACTION_REQUIRED;
        }
        if (flowableFacade.hasDeadLetterJobs(processInstanceId)) {
            return Operation.State.ERROR;
        }
        return Operation.State.RUNNING;
    }

    private Operation.State computeFinalState(String processId) {
        return flowableFacade.hasDeleteReason(processId) ? Operation.State.ABORTED : Operation.State.FINISHED;
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
