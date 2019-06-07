package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudTask;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

@Component("prepareToExecuteTasksStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PrepareToExecuteTasksStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        List<CloudTask> tasksToExecute = StepsUtil.getTasksToExecute(execution.getContext());
        execution.getContext()
            .setVariable(Constants.VAR_PLATFORM_SUPPORTS_TASKS, platformSupportsTasks(execution));

        // Initialize the iteration over the tasks:
        execution.getContext()
            .setVariable(Constants.VAR_TASKS_COUNT, tasksToExecute.size());
        execution.getContext()
            .setVariable(Constants.VAR_TASKS_INDEX, 0);
        execution.getContext()
            .setVariable(Constants.VAR_INDEX_VARIABLE_NAME, Constants.VAR_TASKS_INDEX);
        return StepPhase.DONE;
    }

    @Override
    protected void onStepError(DelegateExecution context, Exception e) throws Exception {
        getStepLogger().error(e, Messages.ERROR_PREPARING_TO_EXECUTE_TASKS_ON_APP, StepsUtil.getApp(context)
            .getName());
        throw e;
    }

    private boolean platformSupportsTasks(ExecutionWrapper execution) {
        CloudControllerClient client = execution.getControllerClient();
        return client.areTasksSupported();
    }

}
