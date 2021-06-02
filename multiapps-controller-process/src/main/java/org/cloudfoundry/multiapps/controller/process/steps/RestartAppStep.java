package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.cf.clients.RecentLogsRetriever;
import org.cloudfoundry.multiapps.controller.core.model.HookPhase;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication.State;

@Named("restartAppStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RestartAppStep extends TimeoutAsyncFlowableStepWithHooks implements BeforeStepHookPhaseProvider {

    @Inject
    protected RecentLogsRetriever recentLogsRetriever;

    @Override
    public StepPhase executePollingStep(ProcessContext context) {
        CloudApplication app = getAppToRestart(context);
        CloudControllerClient client = context.getControllerClient();

        if (isStarted(client, app.getName())) {
            stopApp(client, app);
        }
        startApp(client, app);
        setStartupPollingInfo(context);
        return StepPhase.POLL;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return MessageFormat.format(Messages.ERROR_STARTING_APP_0, getAppToRestart(context).getName());
    }

    protected CloudApplication getAppToRestart(ProcessContext context) {
        return context.getVariable(Variables.APP_TO_PROCESS);
    }

    private void setStartupPollingInfo(ProcessContext context) {
        if (context.getVariable(Variables.START_TIME) == null) {
            context.setVariable(Variables.START_TIME, System.currentTimeMillis());
        }
    }

    private boolean isStarted(CloudControllerClient client, String appName) {
        try {
            CloudApplication app = client.getApplication(appName);
            return app.getState()
                      .equals(State.STARTED);
        } catch (CloudOperationException e) {
            if (e.getStatusCode()
                 .equals(HttpStatus.INTERNAL_SERVER_ERROR)) {
                logger.warn(e.getMessage(), e);
                return false;
            }
            throw e;
        }
    }

    private void stopApp(CloudControllerClient client, CloudApplication app) {
        getStepLogger().info(Messages.STOPPING_APP, app.getName());
        client.stopApplication(app.getName());
    }

    private void startApp(CloudControllerClient client, CloudApplication app) {
        getStepLogger().info(Messages.STARTING_APP, app.getName());
        client.startApplication(app.getName());
    }

    @Override
    public List<HookPhase> getHookPhasesBeforeStep(ProcessContext context) {
        return hooksPhaseBuilder.buildHookPhases(Collections.singletonList(HookPhase.BEFORE_START), context);
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ProcessContext context) {
        return Arrays.asList(new PollStartAppStatusExecution(recentLogsRetriever), new PollExecuteAppStatusExecution(recentLogsRetriever));
    }

    @Override
    public Duration getTimeout(ProcessContext context) {
        return context.getVariable(Variables.START_TIMEOUT);
    }

}
