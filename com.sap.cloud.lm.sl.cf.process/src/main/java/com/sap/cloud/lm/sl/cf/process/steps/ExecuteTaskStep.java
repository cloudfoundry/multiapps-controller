package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudTask;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

@Component("executeTaskStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ExecuteTaskStep extends TimeoutAsyncFlowableStep {

    protected Supplier<Long> currentTimeSupplier = System::currentTimeMillis;

    @Inject
    private RecentLogsRetriever recentLogsRetriever;

    @Override
    protected StepPhase executeAsyncStep(ExecutionWrapper execution) throws Exception {
        CloudApplicationExtended app = StepsUtil.getApp(execution.getContext());
        CloudTask taskToExecute = StepsUtil.getTask(execution.getContext());
        CloudControllerClient client = execution.getControllerClient();

        getStepLogger().info(Messages.EXECUTING_TASK_ON_APP, taskToExecute.getName(), app.getName());
        CloudTask startedTask = client.runTask(app.getName(), taskToExecute);

        StepsUtil.setStartedTask(execution.getContext(), startedTask);
        execution.getContext()
            .setVariable(Constants.VAR_START_TIME, currentTimeSupplier.get());
        return StepPhase.POLL;
    }

    @Override
    protected void onStepError(DelegateExecution context, Exception e) throws Exception {
        CloudApplicationExtended app = StepsUtil.getApp(context);
        CloudTask taskToExecute = StepsUtil.getTask(context);
        getStepLogger().error(e, Messages.ERROR_EXECUTING_TASK_ON_APP, taskToExecute.getName(), app.getName());
        throw e;
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ExecutionWrapper execution) {
        recentLogsRetriever.setFailSafe(true);
        return Arrays.asList(new PollExecuteTaskStatusExecution(recentLogsRetriever, currentTimeSupplier));
    }

    @Override
    public Integer getTimeout(DelegateExecution context) {
        return StepsUtil.getInteger(context, Constants.PARAM_START_TIMEOUT, Constants.DEFAULT_START_TIMEOUT);
    }

}
