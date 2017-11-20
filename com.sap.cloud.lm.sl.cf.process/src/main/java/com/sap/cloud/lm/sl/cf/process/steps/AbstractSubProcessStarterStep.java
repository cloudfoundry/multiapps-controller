package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Map;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.runtime.ProcessInstance;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.activiti.ActivitiFacade;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.SLException;

public abstract class AbstractSubProcessStarterStep extends AsyncActivitiStep {

    @Inject
    private ActivitiFacade actvitiFacade;

    @Override
    protected ExecutionStatus executeAsyncStep(ExecutionWrapper execution) throws Exception {
        try {
            String userId = StepsUtil.determineCurrentUser(execution.getContext(), getStepLogger());
            int nextIndex = getStepIndex(execution.getContext());
            execution.getContext().setVariable(getIterationVariableName(), getIterationVariable(execution.getContext(), nextIndex));
            Map<String, Object> parentProcessVariables = execution.getContext().getVariables();
            parentProcessVariables.put(Constants.VAR_PARENTPROCESS_ID, execution.getContext().getProcessInstanceId());
            ProcessInstance subProcessInstance = actvitiFacade.startProcess(userId, getProcessDefinitionKey(), parentProcessVariables);
            StepsUtil.setSubProcessId(execution.getContext(), subProcessInstance.getProcessInstanceId());

            StepsUtil.setStepPhase(execution, StepPhase.POLL);
            return ExecutionStatus.RUNNING;
        } catch (Exception e) {
            getStepLogger().error(e, "Error starting sub-process");
            StepsUtil.setStepPhase(execution, StepPhase.RETRY);
            throw new SLException(e);
        }
    }

    protected abstract String getIterationVariableName();

    protected abstract String getProcessDefinitionKey();

    protected abstract Object getIterationVariable(DelegateExecution context, int index);

}
