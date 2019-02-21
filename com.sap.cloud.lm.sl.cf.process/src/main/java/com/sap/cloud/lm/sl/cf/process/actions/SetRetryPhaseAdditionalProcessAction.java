package com.sap.cloud.lm.sl.cf.process.actions;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.flowable.AdditionalProcessAction;
import com.sap.cloud.lm.sl.cf.core.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.cf.core.flowable.RetryProcessAction;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.steps.StepPhase;

@Component
public class SetRetryPhaseAdditionalProcessAction implements AdditionalProcessAction {

    private FlowableFacade flowableFacade;

    @Inject
    public SetRetryPhaseAdditionalProcessAction(FlowableFacade flowableFacade) {
        this.flowableFacade = flowableFacade;
    }

    @Override
    public void executeAdditionalProcessAction(String processInstanceId) {
        flowableFacade.getActiveProcessExecutions(processInstanceId)
            .stream()
            .map(execution -> execution.getProcessInstanceId())
            .forEach(executionProcessId -> flowableFacade.getProcessEngine()
                .getRuntimeService()
                .setVariable(executionProcessId, Constants.VAR_STEP_PHASE, StepPhase.RETRY));
    }

    @Override
    public String getApplicableActionId() {
        return RetryProcessAction.ACTION_ID_RETRY;
    }

}
