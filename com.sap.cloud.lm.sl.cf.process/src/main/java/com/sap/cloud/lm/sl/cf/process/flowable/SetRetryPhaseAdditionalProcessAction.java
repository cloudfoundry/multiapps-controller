package com.sap.cloud.lm.sl.cf.process.flowable;

import javax.inject.Inject;
import javax.inject.Named;

import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.runtime.Execution;

import com.sap.cloud.lm.sl.cf.process.steps.StepPhase;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

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
                      .map(this::toExecutionEntityImpl)
                      .filter(executionEntityImpl -> executionEntityImpl.getDeadLetterJobCount() > 0)
                      .map(ExecutionEntityImpl::getProcessInstanceId)
                      .forEach(executionProcessId -> flowableFacade.getProcessEngine()
                                                                   .getRuntimeService()
                                                                   .setVariable(executionProcessId, Variables.STEP_PHASE.getName(),
                                                                                StepPhase.RETRY.toString()));
    }

    private ExecutionEntityImpl toExecutionEntityImpl(Execution e) {
        return (ExecutionEntityImpl) e;
    }

    @Override
    public String getApplicableActionId() {
        return RetryProcessAction.ACTION_ID_RETRY;
    }

}
