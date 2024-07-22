package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.List;
import java.util.function.LongSupplier;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientFactory;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudTask;

@Named("executeTaskStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ExecuteTaskStep extends TimeoutAsyncFlowableStep {

    protected LongSupplier currentTimeSupplier = System::currentTimeMillis;

    @Inject
    private CloudControllerClientFactory clientFactory;
    @Inject
    private TokenService tokenService;

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
        return List.of(new PollExecuteTaskStatusExecution(clientFactory, tokenService));
    }

    @Override
    public Duration getTimeout(ProcessContext context) {
        // TODO: This is a temporary solution because there are clients that have very long running tasks
        // timeouts should be more granular for the different types of steps: LMCROSSITXSADEPLOY-2424, LMCROSSITXSADEPLOY-2425
        return Duration.ofHours(12);
    }

}
