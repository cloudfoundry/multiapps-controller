package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.function.LongSupplier;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudTask;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

@Named("executeTaskStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ExecuteTaskStep extends TimeoutAsyncFlowableStep {

    protected LongSupplier currentTimeSupplier = System::currentTimeMillis;

    @Inject
    private RecentLogsRetriever recentLogsRetriever;

    @Override
    protected StepPhase executeAsyncStep(ExecutionWrapper execution) {
        CloudApplicationExtended app = StepsUtil.getApp(execution.getContext());
        CloudTask taskToExecute = StepsUtil.getTask(execution.getContext());
        CloudControllerClient client = execution.getControllerClient();

        getStepLogger().info(Messages.EXECUTING_TASK_ON_APP, taskToExecute.getName(), app.getName());
        CloudTask startedTask = client.runTask(app.getName(), taskToExecute);

        StepsUtil.setStartedTask(execution.getContext(), startedTask);
        execution.getContext()
                 .setVariable(Constants.VAR_START_TIME, currentTimeSupplier.getAsLong());
        return StepPhase.POLL;
    }

    @Override
    protected String getStepErrorMessage(DelegateExecution context) {
        CloudApplicationExtended app = StepsUtil.getApp(context);
        CloudTask taskToExecute = StepsUtil.getTask(context);
        return MessageFormat.format(Messages.ERROR_EXECUTING_TASK_ON_APP, taskToExecute.getName(), app.getName());
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ExecutionWrapper execution) {
        return Collections.singletonList(new PollExecuteTaskStatusExecution(recentLogsRetriever));
    }

    @Override
    public Integer getTimeout(DelegateExecution context) {
        return StepsUtil.getInteger(context, Constants.PARAM_START_TIMEOUT, Constants.DEFAULT_START_TIMEOUT);
    }

}
