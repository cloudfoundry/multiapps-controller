package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudTask;
import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Component("executeTaskStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ExecuteTaskStep extends AsyncActivitiStep {

    protected Supplier<Long> currentTimeSupplier = () -> System.currentTimeMillis();

    @Inject
    private RecentLogsRetriever recentLogsRetriever;

    @Override
    protected String getIndexVariable() {
        return Constants.VAR_TASKS_INDEX;
    }

    @Override
    protected ExecutionStatus executeAsyncStep(ExecutionWrapper execution) throws Exception {
        getStepLogger().logActivitiTask();

        CloudApplicationExtended app = StepsUtil.getApp(execution.getContext());
        CloudTask taskToExecute = StepsUtil.getTask(execution.getContext());
        try {
            return attemptToExecuteTask(execution, app, taskToExecute);
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            getStepLogger().error(e, Messages.ERROR_EXECUTING_TASK_ON_APP, taskToExecute.getName(), app.getName());
            throw e;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_EXECUTING_TASK_ON_APP, taskToExecute.getName(), app.getName());
            throw e;
        }
    }

    private ExecutionStatus attemptToExecuteTask(ExecutionWrapper execution, CloudApplication app, CloudTask taskToExecute) {
        ClientExtensions clientExtensions = execution.getClientExtensions();

        getStepLogger().info(Messages.EXECUTING_TASK_ON_APP, taskToExecute.getName(), app.getName());
        CloudTask startedTask = runTask(clientExtensions, app, taskToExecute);

        StepsUtil.setStartedTask(execution.getContext(), startedTask);
        execution.getContext().setVariable(Constants.VAR_START_TIME, currentTimeSupplier.get());
        StepsUtil.setStepPhase(execution, StepPhase.POLL);
        return ExecutionStatus.RUNNING;
    }

    private CloudTask runTask(ClientExtensions clientExtensions, CloudApplication app, CloudTask task) {
        return clientExtensions.runTask(app.getName(), task.getName(), task.getCommand(), task.getEnvironmentVariables());
    }

    @Override
    protected List<AsyncStepOperation> getAsyncStepOperations() {
        return Arrays.asList(new PollExecuteTaskStatusStep(recentLogsRetriever, currentTimeSupplier));
    }

}
