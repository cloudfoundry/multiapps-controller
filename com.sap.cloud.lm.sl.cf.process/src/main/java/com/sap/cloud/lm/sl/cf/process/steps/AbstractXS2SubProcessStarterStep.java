package com.sap.cloud.lm.sl.cf.process.steps;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.activiti.ActivitiFacade;

public abstract class AbstractXS2SubProcessStarterStep extends AbstractXS2ProcessStepWithBridge {

    static final Logger LOGGER = LoggerFactory.getLogger(StartAppDeploySubProcessStep.class);

    @Inject
    private ActivitiFacade actvitiFacade;

    @Override
    protected ExecutionStatus pollStatusInternal(DelegateExecution context) throws Exception {
        try {
            String userId = StepsUtil.determineCurrentUser(context, LOGGER, processLoggerProviderFactory);

            int nextIndex = (int) context.getVariable(getIndexVariable());
            context.setVariable(getIterationVariableName(), getIterationVariable(context, nextIndex));
            ProcessInstance subProcessInstance = actvitiFacade.startProcessInstance(userId, getProcessDefinitionKey(),
                context.getVariables());


            StepsUtil.setSubProcessId(context, subProcessInstance.getProcessInstanceId());

            return ExecutionStatus.SUCCESS;
        } catch (Exception e) {
            error(context, "Error starting sub-process", e, LOGGER);
            throw new SLException(e);
        }
    }

    protected abstract String getIterationVariableName();

    protected abstract String getProcessDefinitionKey();

    protected abstract Object getIterationVariable(DelegateExecution context, int index);

}
