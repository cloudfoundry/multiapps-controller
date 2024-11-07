package org.cloudfoundry.multiapps.controller.process.flowable;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.cloudfoundry.multiapps.controller.persistence.model.ProgressMessage.ProgressMessageType;
import org.cloudfoundry.multiapps.controller.persistence.services.ProgressMessageService;
import org.flowable.engine.runtime.Execution;

@Named
public class RetryProcessAdditionalAction implements AdditionalProcessAction {

    private final FlowableFacade flowableFacade;
    private final ProgressMessageService progressMessageService;

    @Inject
    public RetryProcessAdditionalAction(FlowableFacade flowableFacade, ProgressMessageService progressMessageService) {
        this.progressMessageService = progressMessageService;
        this.flowableFacade = flowableFacade;
    }

    @Override
    public void executeAdditionalProcessAction(String processInstanceId) {
        List<String> failedActivityIds = findFailedActivityIds(processInstanceId);
        for (String failedActivityId : failedActivityIds) {
            progressMessageService.createQuery()
                                  .processId(processInstanceId)
                                  .taskId(failedActivityId)
                                  .type(ProgressMessageType.ERROR)
                                  .delete();
        }
    }

    private List<String> findFailedActivityIds(String superProcessInstanceId) {
        List<Execution> executionsForProcess = flowableFacade.getActiveProcessExecutions(superProcessInstanceId);
        return executionsForProcess.stream()
                                   .map(Execution::getActivityId)
                                   .collect(Collectors.toList());
    }

    @Override
    public Action getApplicableAction() {
        return Action.RETRY;
    }

}
