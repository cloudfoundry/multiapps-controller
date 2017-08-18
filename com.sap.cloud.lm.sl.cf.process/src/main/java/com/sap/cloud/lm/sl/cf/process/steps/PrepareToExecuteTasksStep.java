package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudTask;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.OneOffTasksSupportChecker;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.LoopStepMetadata;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("prepareToExecuteTasksStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PrepareToExecuteTasksStep extends AbstractXS2ProcessStep {

    public static StepMetadata getMetadata() {
        return LoopStepMetadata.builder().id("prepareToExecuteTasksTask").displayName("Prepare To Execute Tasks").description(
            "Prepare To Execute Tasks").children(ExecuteTaskStep.getMetadata()).countVariable(Constants.VAR_TASKS_COUNT).build();
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) {
        getStepLogger().logActivitiTask();

        CloudApplicationExtended app = StepsUtil.getApp(context);
        List<CloudTask> tasksToExecute = app.getTasks();
        try {
            return attemptToPrepareExecutionOfTasks(context, tasksToExecute);
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            getStepLogger().error(e, Messages.ERROR_PREPARING_TO_EXECUTE_TASKS_ON_APP, app.getName());
            throw e;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_PREPARING_TO_EXECUTE_TASKS_ON_APP, app.getName());
            throw e;
        }
    }

    private ExecutionStatus attemptToPrepareExecutionOfTasks(DelegateExecution context, List<CloudTask> tasksToExecute) {
        StepsUtil.setTasksToExecute(context, tasksToExecute);

        context.setVariable(Constants.VAR_PLATFORM_SUPPORTS_TASKS, platformSupportsTasks(context));

        // Initialize the iteration over the tasks:
        context.setVariable(Constants.VAR_TASKS_COUNT, tasksToExecute.size());
        context.setVariable(Constants.VAR_TASKS_INDEX, 0);
        context.setVariable(Constants.VAR_INDEX_VARIABLE_NAME, Constants.VAR_TASKS_INDEX);

        context.setVariable(Constants.VAR_CONTROLLER_POLLING_INTERVAL, ConfigurationUtil.getControllerPollingInterval());

        return ExecutionStatus.SUCCESS;
    }

    private boolean platformSupportsTasks(DelegateExecution context) {
        return new OneOffTasksSupportChecker().areOneOffTasksSupported(getCloudFoundryClient(context));
    }

}
