package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.function.LongSupplier;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudTask;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

@Named("executeTaskStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ExecuteTaskStep extends TimeoutAsyncFlowableStep {

    protected LongSupplier currentTimeSupplier = System::currentTimeMillis;

    @Inject
    private RecentLogsRetriever recentLogsRetriever;

    @Override
    protected StepPhase executeAsyncStep(ProcessContext context) {
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
        CloudTask taskToExecute = StepsUtil.getTask(context);
        CloudControllerClient client = context.getControllerClient();

        getStepLogger().info(Messages.EXECUTING_TASK_ON_APP, taskToExecute.getName(), app.getName());
        CloudTask startedTask = client.runTask(app.getName(), taskToExecute);
        context.setVariable(Variables.STARTED_TASK, startedTask);
        context.setVariable(Variables.START_TIME, currentTimeSupplier.getAsLong());
        return StepPhase.POLL;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
        CloudTask taskToExecute = StepsUtil.getTask(context);
        return MessageFormat.format(Messages.ERROR_EXECUTING_TASK_0_ON_APP_1, taskToExecute.getName(), app.getName());
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ProcessContext context) {
        return Collections.singletonList(new PollExecuteTaskStatusExecution(recentLogsRetriever));
    }

    @Override
    public Integer getTimeout(ProcessContext context) {
        return context.getVariable(Variables.START_TIMEOUT);
    }

}
