package com.sap.cloud.lm.sl.cf.process.steps;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.runtime.ProcessInstance;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.activiti.ActivitiFacade;

public abstract class AbstractXS2SubProcessStarterStep extends AbstractXS2ProcessStepWithBridge {

    @Inject
    private ActivitiFacade actvitiFacade;

    @Override
    protected ExecutionStatus pollStatusInternal(DelegateExecution context) throws Exception {
        try {
            String userId = StepsUtil.determineCurrentUser(context, getStepLogger());

            int nextIndex = (int) context.getVariable(getIndexVariable());
            context.setVariable(getIterationVariableName(), getIterationVariable(context, nextIndex));
            ProcessInstance subProcessInstance = actvitiFacade.startProcessInstance(userId, getProcessDefinitionKey(),
                context.getVariables());

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
