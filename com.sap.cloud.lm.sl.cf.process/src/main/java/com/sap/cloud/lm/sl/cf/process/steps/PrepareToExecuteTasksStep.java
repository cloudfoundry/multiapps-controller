package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.List;

import javax.inject.Named;

import org.cloudfoundry.client.lib.domain.CloudTask;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

@Named("prepareToExecuteTasksStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PrepareToExecuteTasksStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        List<CloudTask> tasksToExecute = StepsUtil.getTasksToExecute(execution.getContext());
        execution.getContext()
                 .setVariable(Constants.VAR_TASKS_COUNT, tasksToExecute.size());
        execution.getContext()
                 .setVariable(Constants.VAR_TASKS_INDEX, 0);
        execution.getContext()
                 .setVariable(Constants.VAR_INDEX_VARIABLE_NAME, Constants.VAR_TASKS_INDEX);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ExecutionWrapper execution) {
        return MessageFormat.format(Messages.ERROR_PREPARING_TO_EXECUTE_TASKS_ON_APP, execution.getVariable(Variables.APP_TO_PROCESS)
                                                                                               .getName());
    }

}
