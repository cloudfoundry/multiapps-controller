package com.sap.cloud.lm.sl.cf.process.util;

import java.util.List;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Named;

import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent;
import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent.EventType;
import com.sap.cloud.lm.sl.cf.core.persistence.service.HistoricOperationEventService;
import com.sap.cloud.lm.sl.cf.process.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation.State;

@Named
public class ProcessHelper {

    private final FlowableFacade flowableFacade;
    private final HistoricOperationEventService historicOperationEventService;

    @Inject
    public ProcessHelper(FlowableFacade flowableFacade, HistoricOperationEventService historicOperationEventService) {
        this.flowableFacade = flowableFacade;
        this.historicOperationEventService = historicOperationEventService;
    }

    public Operation.State computeProcessState(String processId) {
        if (isInReceiveTask(processId)) {
            return State.ACTION_REQUIRED;
        }

        List<HistoricOperationEvent> operationEvents = getHistoricOperationEventByProcessId(processId);
        if (isInAbortedState(operationEvents)) {
            return State.ABORTED;
        }

        if (isInFinishedState(operationEvents)) {
            return State.FINISHED;
        }

        if (isInErrorState(operationEvents)) {
            return State.ERROR;
        }

        return State.RUNNING;
    }

    private boolean isInReceiveTask(String processId) {
        return flowableFacade.isProcessInstanceAtReceiveTask(processId);
    }

    public List<HistoricOperationEvent> getHistoricOperationEventByProcessId(String processId) {
        return historicOperationEventService.createQuery()
                                            .processId(processId)
                                            .list();
    }

    private boolean isInAbortedState(List<HistoricOperationEvent> operationEvents) {
        return isInState(operationEvents, event -> event.getType() == EventType.ABORTED);
    }

    private boolean isInFinishedState(List<HistoricOperationEvent> operationEvents) {
        return isInState(operationEvents, event -> event.getType() == EventType.FINISHED);
    }

    private boolean isInErrorState(List<HistoricOperationEvent> operationEvents) {
        return isInState(operationEvents, event -> event.getType() == EventType.FAILED_BY_CONTENT_ERROR
            || event.getType() == EventType.FAILED_BY_INFRASTRUCTURE_ERROR);
    }

    private boolean isInState(List<HistoricOperationEvent> operationEvents, Predicate<? super HistoricOperationEvent> statePredicate) {
        return operationEvents.stream()
                              .anyMatch(statePredicate);
    }

}
