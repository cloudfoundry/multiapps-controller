package com.sap.cloud.lm.sl.cf.process.util;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.flowable.engine.runtime.ProcessInstance;

import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent;
import com.sap.cloud.lm.sl.cf.core.persistence.service.HistoricOperationEventService;
import com.sap.cloud.lm.sl.cf.process.flowable.FlowableFacade;

@Named
public class ProcessHelper {

    private final FlowableFacade flowableFacade;
    private final HistoricOperationEventService historicOperationEventService;

    @Inject
    public ProcessHelper(FlowableFacade flowableFacade, HistoricOperationEventService historicOperationEventService) {
        this.flowableFacade = flowableFacade;
        this.historicOperationEventService = historicOperationEventService;
    }

    public boolean isAborted(String processId) {
        List<HistoricOperationEvent> historicOperationEvents = getHistoricOperationEventByProcessId(processId);
        return historicOperationEvents.stream()
                                      .map(HistoricOperationEvent::getType)
                                      .anyMatch(historicOperationEvent -> Objects.equals(historicOperationEvent,
                                                                                         HistoricOperationEvent.EventType.ABORTED));
    }

    public boolean isAtReceiveTask(String processId) {
        return flowableFacade.isProcessInstanceAtReceiveTask(processId);
    }

    public boolean isInErrorState(String processId) {
        return flowableFacade.hasDeadLetterJobs(processId);
    }

    public Optional<ProcessInstance> findProcessInstanceById(String processId) {
        return Optional.ofNullable(flowableFacade.getProcessInstance(processId));
    }

    public List<HistoricOperationEvent> getHistoricOperationEventByProcessId(String processId) {
        return historicOperationEventService.createQuery()
                                            .processId(processId)
                                            .list();
    }

}
