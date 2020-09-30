package org.cloudfoundry.multiapps.controller.process.util;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.api.model.Operation.State;
import org.cloudfoundry.multiapps.controller.persistence.model.HistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.persistence.services.HistoricOperationEventService;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.flowable.engine.runtime.ProcessInstance;

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
        if (isInAbortedState(processId)) {
            return State.ABORTED;
        }

        if (isInErrorState(processId)) {
            return State.ERROR;
        }

        if (isInReceiveTask(processId)) {
            return State.ACTION_REQUIRED;
        }

        if (isInRunningState(processId)) {
            return State.RUNNING;
        }
        return State.FINISHED;
    }

    private boolean isInReceiveTask(String processId) {
        return flowableFacade.isProcessInstanceAtReceiveTask(processId);
    }

    private boolean isInAbortedState(String processId) {
        List<HistoricOperationEvent> historicOperationEvents = getHistoricOperationEventByProcessId(processId);
        return historicOperationEvents.stream()
                                      .anyMatch(event -> event.getType() == HistoricOperationEvent.EventType.ABORTED);
    }

    public List<HistoricOperationEvent> getHistoricOperationEventByProcessId(String processId) {
        return historicOperationEventService.createQuery()
                                            .processId(processId)
                                            .list();
    }

    private boolean isInErrorState(String processId) {
        return flowableFacade.hasDeadLetterJobs(processId);
    }

    private boolean isInRunningState(String processId) {
        Optional<ProcessInstance> processInstanceOptional = getProcessInstance(processId);
        return processInstanceOptional.isPresent();
    }

    private Optional<ProcessInstance> getProcessInstance(String processId) {
        return Optional.ofNullable(flowableFacade.getProcessInstance(processId));
    }

}
