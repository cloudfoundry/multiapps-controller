package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudTask;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.process.Constants;

@Component("prepareToExecuteTasksStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PrepareToExecuteTasksStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        CloudApplicationExtended app = StepsUtil.getApp(execution.getContext());
        List<CloudTask> tasksToExecute = StepsUtil.getTasksToExecute(execution.getContext());
        try {
            return attemptToPrepareExecutionOfTasks(execution, tasksToExecute);
        } catch (CloudOperationException coe) {
            throw new CloudControllerException(coe);
        }
    }

    private StepPhase attemptToPrepareExecutionOfTasks(ExecutionWrapper execution, List<CloudTask> tasksToExecute) {
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

    private boolean platformSupportsTasks(ExecutionWrapper execution) {
        CloudControllerClient client = execution.getControllerClient();
        return client.areTasksSupported();
    }

}
