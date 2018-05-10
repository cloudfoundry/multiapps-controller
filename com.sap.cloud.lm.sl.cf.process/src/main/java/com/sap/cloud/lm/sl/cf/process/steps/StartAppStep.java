package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.Arrays;
import java.util.List;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

@Component("startAppStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class StartAppStep extends TimeoutAsyncActivitiStep {

    @Autowired
    protected RecentLogsRetriever recentLogsRetriever;
    @Autowired
    protected ApplicationConfiguration configuration;

    @Override
    protected String getIndexVariable() {
        return Constants.VAR_APPS_INDEX;
    }

    @Override
    public StepPhase executeAsyncStep(ExecutionWrapper execution) {
        CloudApplication app = getAppToStart(execution.getContext());
        try {
            attemptToStartApp(execution, app);
        } catch (CloudFoundryException cfe) {
            CloudControllerException e = new CloudControllerException(cfe);
            onError(format(Messages.ERROR_STARTING_APP_1, app.getName()), e);
            throw e;
        }
        return StepPhase.POLL;
    }

    protected void onError(String message, Exception e) {
        getStepLogger().error(e, message);
    }

    protected void onError(String message) {
        getStepLogger().error(message);
    }

    private void attemptToStartApp(ExecutionWrapper execution, CloudApplication app) {
        CloudFoundryOperations client = execution.getCloudFoundryClient();

        if (isAppStarted(client, app.getName())) {
            stopApp(client, app);
        }
        StartingInfo startingInfo = startApp(execution, client, app);
        StepsUtil.setStartingInfo(execution.getContext(), startingInfo);
        if (execution.getContext()
            .getVariable(Constants.VAR_START_TIME) == null) {
            execution.getContext()
                .setVariable(Constants.VAR_START_TIME, System.currentTimeMillis());
        }
        if (execution.getContext()
            .getVariable(Constants.VAR_OFFSET) == null) {
            execution.getContext()
                .setVariable(Constants.VAR_OFFSET, 0);
        }
    }

    protected CloudApplication getAppToStart(DelegateExecution context) {
        return StepsUtil.getApp(context);
    }

    private boolean isAppStarted(CloudFoundryOperations client, String appName) {
        try {
            CloudApplication app2 = client.getApplication(appName);
            return app2.getState()
                .equals(AppState.STARTED);
        } catch (CloudFoundryException e) {
            if (e.getStatusCode()
                .equals(HttpStatus.INTERNAL_SERVER_ERROR)) {
                LOGGER.warn(e.getMessage(), e);
                return false;
            }
            throw e;
        }
    }

    private void stopApp(CloudFoundryOperations client, CloudApplication app) {
        getStepLogger().info(Messages.STOPPING_APP, app.getName());
        client.stopApplication(app.getName());
    }

    private StartingInfo startApp(ExecutionWrapper execution, CloudFoundryOperations client, CloudApplication app) {
        ClientExtensions clientExtensions = execution.getClientExtensions();
        getStepLogger().info(Messages.STARTING_APP, app.getName());
        if (clientExtensions != null) {
            return clientExtensions.startApplication(app.getName(), false);
        }
        return client.startApplication(app.getName());
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions() {
        return Arrays.asList(new PollStartAppStatusExecution(recentLogsRetriever, configuration),
            new PollExecuteAppStatusExecution(recentLogsRetriever));
    }

    @Override
    public Integer getTimeout(DelegateExecution context) {
        return StepsUtil.getVariableOrDefault(context, Constants.PARAM_START_TIMEOUT, Constants.DEFAULT_START_TIMEOUT);
    }

}
