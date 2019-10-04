package com.sap.cloud.lm.sl.cf.process.action;

import javax.inject.Inject;
import javax.inject.Named;

import org.flowable.engine.runtime.Execution;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.flowable.AdditionalProcessAction;
import com.sap.cloud.lm.sl.cf.process.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.cf.process.flowable.RetryProcessAction;
import com.sap.cloud.lm.sl.cf.process.steps.StepPhase;

@Named
public class SetRetryPhaseAdditionalProcessAction implements AdditionalProcessAction {

    private final FlowableFacade flowableFacade;

    @Inject
    public SetRetryPhaseAdditionalProcessAction(FlowableFacade flowableFacade) {
        this.flowableFacade = flowableFacade;
    }

    @Override
    public void executeAdditionalProcessAction(String processInstanceId) {
        flowableFacade.getActiveProcessExecutions(processInstanceId)
                      .stream()
                      .map(Execution::getProcessInstanceId)
                      .forEach(executionProcessId -> flowableFacade.getProcessEngine()
                                                                   .getRuntimeService()
                                                                   .setVariable(executionProcessId, Constants.VAR_STEP_PHASE,
                                                                                StepPhase.RETRY.toString()));
    }

    @Override
    public String getApplicableActionId() {
        return RetryProcessAction.ACTION_ID_RETRY;
    }

}
