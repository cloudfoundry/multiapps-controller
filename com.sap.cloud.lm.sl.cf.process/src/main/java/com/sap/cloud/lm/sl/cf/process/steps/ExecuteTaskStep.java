package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.XsCloudControllerClient;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudTask;
import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Component("executeTaskStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ExecuteTaskStep extends TimeoutAsyncActivitiStep {

    protected Supplier<Long> currentTimeSupplier = System::currentTimeMillis;

    @Inject
    private RecentLogsRetriever recentLogsRetriever;

    @Override
    protected String getIndexVariable() {
        return Constants.VAR_TASKS_INDEX;
    }

    @Override
    protected StepPhase executeAsyncStep(ExecutionWrapper execution) throws Exception {
        CloudApplicationExtended app = StepsUtil.getApp(execution.getContext());
        CloudTask taskToExecute = StepsUtil.getTask(execution.getContext());
        try {
            return attemptToExecuteTask(execution, app, taskToExecute);
        } catch (CloudOperationException coe) {
            CloudControllerException e = new CloudControllerException(coe);
            getStepLogger().error(e, Messages.ERROR_EXECUTING_TASK_ON_APP, taskToExecute.getName(), app.getName());
            throw e;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_EXECUTING_TASK_ON_APP, taskToExecute.getName(), app.getName());
            throw e;
        }
    }

    private StepPhase attemptToExecuteTask(ExecutionWrapper execution, CloudApplication app, CloudTask taskToExecute) {
        XsCloudControllerClient xsClient = execution.getXsControllerClient();

        getStepLogger().info(Messages.EXECUTING_TASK_ON_APP, taskToExecute.getName(), app.getName());
        CloudTask startedTask = runTask(xsClient, app, taskToExecute);

        StepsUtil.setStartedTask(execution.getContext(), startedTask);
        execution.getContext()
            .setVariable(Constants.VAR_START_TIME, currentTimeSupplier.get());
        return StepPhase.POLL;
    }

    private CloudTask runTask(XsCloudControllerClient xsClient, CloudApplication app, CloudTask task) {
        return xsClient.runTask(app.getName(), task.getName(), task.getCommand(), task.getEnvironmentVariables());
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ExecutionWrapper execution) {
        return Arrays.asList(new PollExecuteTaskStatusExecution(recentLogsRetriever, currentTimeSupplier));
    }

    @Override
    public Integer getTimeout(DelegateExecution context) {
        return Constants.DEFAULT_START_TIMEOUT;
    }

}
