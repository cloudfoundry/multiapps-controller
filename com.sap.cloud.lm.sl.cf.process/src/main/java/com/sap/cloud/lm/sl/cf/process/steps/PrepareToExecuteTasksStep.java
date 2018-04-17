package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudTask;
import com.sap.cloud.lm.sl.cf.core.util.Configuration;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.OneOffTasksSupportChecker;
import com.sap.cloud.lm.sl.common.SLException;

@Component("prepareToExecuteTasksStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PrepareToExecuteTasksStep extends SyncActivitiStep {

    @Inject
    private Configuration configuration;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        CloudApplicationExtended app = StepsUtil.getApp(execution.getContext());
        List<CloudTask> tasksToExecute = app.getTasks();
        try {
            return attemptToPrepareExecutionOfTasks(execution, tasksToExecute);
        } catch (CloudFoundryException cfe) {
            CloudControllerException e = new CloudControllerException(cfe);
            getStepLogger().error(e, Messages.ERROR_PREPARING_TO_EXECUTE_TASKS_ON_APP, app.getName());
            throw e;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_PREPARING_TO_EXECUTE_TASKS_ON_APP, app.getName());
            throw e;
        }
    }

    private StepPhase attemptToPrepareExecutionOfTasks(ExecutionWrapper execution, List<CloudTask> tasksToExecute) {
        StepsUtil.setTasksToExecute(execution.getContext(), tasksToExecute);

        execution.getContext()
            .setVariable(Constants.VAR_PLATFORM_SUPPORTS_TASKS, platformSupportsTasks(execution));

        // Initialize the iteration over the tasks:
        execution.getContext()
            .setVariable(Constants.VAR_TASKS_COUNT, tasksToExecute.size());
        execution.getContext()
            .setVariable(Constants.VAR_TASKS_INDEX, 0);
        execution.getContext()
            .setVariable(Constants.VAR_INDEX_VARIABLE_NAME, Constants.VAR_TASKS_INDEX);

        execution.getContext()
            .setVariable(Constants.VAR_CONTROLLER_POLLING_INTERVAL, configuration.getControllerPollingInterval());

        return StepPhase.DONE;
    }

    private boolean platformSupportsTasks(ExecutionWrapper execution) {
        return new OneOffTasksSupportChecker().areOneOffTasksSupported(execution.getCloudFoundryClient());
    }

}
