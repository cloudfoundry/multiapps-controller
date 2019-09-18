package com.sap.cloud.lm.sl.cf.process.flowable;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.flowable.engine.runtime.Execution;

import com.sap.cloud.lm.sl.cf.core.persistence.service.ProgressMessageService;
import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage.ProgressMessageType;

@Named
public class RetryProcessAdditionalAction implements AdditionalProcessAction {

    private FlowableFacade flowableFacade;
    private ProgressMessageService progressMessageService;

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
    public String getApplicableActionId() {
        return RetryProcessAction.ACTION_ID_RETRY;
    }

}
