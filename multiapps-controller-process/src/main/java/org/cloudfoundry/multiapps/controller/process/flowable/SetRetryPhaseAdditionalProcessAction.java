package org.cloudfoundry.multiapps.controller.process.flowable;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.process.steps.StepPhase;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.runtime.Execution;

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
                      .forEach(this::setRetryPhaseForProcess);
    }

    private ExecutionEntityImpl toExecutionEntityImpl(Execution execution) {
        return (ExecutionEntityImpl) execution;
    }

    private void setRetryPhaseForProcess(String executionProcessId) {
        flowableFacade.getProcessEngine()
                      .getRuntimeService()
                      .setVariable(executionProcessId, Variables.STEP_PHASE.getName(), StepPhase.RETRY.toString());
    }

    @Override
    public Action getApplicableAction() {
        return Action.RETRY;
    }

}
