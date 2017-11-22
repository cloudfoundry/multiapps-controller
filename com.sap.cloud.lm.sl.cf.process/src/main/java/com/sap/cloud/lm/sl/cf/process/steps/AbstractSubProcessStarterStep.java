package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Map;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.runtime.ProcessInstance;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.activiti.ActivitiFacade;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.SLException;

public abstract class AbstractSubProcessStarterStep extends AbstractProcessStep {

    @Inject
    private ActivitiFacade actvitiFacade;

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws Exception {
        try {
            String userId = StepsUtil.determineCurrentUser(context, getStepLogger());

            int nextIndex = (int) context.getVariable(getIndexVariable());
            context.setVariable(getIterationVariableName(), getIterationVariable(context, nextIndex));
            Map<String, Object> parentProcessVariables = context.getVariables();
            parentProcessVariables.put(Constants.VAR_PARENTPROCESS_ID, context.getProcessInstanceId());
            ProcessInstance subProcessInstance = actvitiFacade.startProcess(userId, getProcessDefinitionKey(),
                parentProcessVariables);
            StepsUtil.setSubProcessId(context, subProcessInstance.getProcessInstanceId());

            return ExecutionStatus.SUCCESS;
        } catch (Exception e) {
            getStepLogger().error(e, "Error starting sub-process");
            throw new SLException(e);
        }
    }

    protected abstract String getIterationVariableName();

    protected abstract String getProcessDefinitionKey();

    protected abstract Object getIterationVariable(DelegateExecution context, int index);

}
