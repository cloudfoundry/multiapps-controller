package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.State;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

@Named("restartAppStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RestartAppStep extends TimeoutAsyncFlowableStep {

    @Inject
    protected RecentLogsRetriever recentLogsRetriever;

    @Override
    public StepPhase executeAsyncStep(ExecutionWrapper execution) {
        CloudApplication app = getAppToRestart(execution);
        CloudControllerClient client = execution.getControllerClient();

        if (isStarted(client, app.getName())) {
            stopApp(client, app);
        }
        StartingInfo startingInfo = startApp(client, app);
        setStartupPollingInfo(execution.getContext(), startingInfo);
        return StepPhase.POLL;
    }

    @Override
    protected String getStepErrorMessage(ExecutionWrapper execution) {
        return MessageFormat.format(Messages.ERROR_STARTING_APP_0, getAppToRestart(execution).getName());
    }

    protected CloudApplication getAppToRestart(ExecutionWrapper execution) {
        return execution.getVariable(Variables.APP_TO_PROCESS);
    }

    private void setStartupPollingInfo(DelegateExecution context, StartingInfo startingInfo) {
        StepsUtil.setStartingInfo(context, startingInfo);
        if (context.getVariable(Constants.VAR_START_TIME) == null) {
            context.setVariable(Constants.VAR_START_TIME, System.currentTimeMillis());
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

    private StartingInfo startApp(CloudControllerClient client, CloudApplication app) {
        getStepLogger().info(Messages.STARTING_APP, app.getName());
        return client.startApplication(app.getName());
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ExecutionWrapper execution) {
        return Arrays.asList(new PollStartAppStatusExecution(recentLogsRetriever), new PollExecuteAppStatusExecution(recentLogsRetriever));
    }

    @Override
    public Integer getTimeout(ExecutionWrapper execution) {
        return StepsUtil.getInteger(execution.getContext(), Constants.PARAM_START_TIMEOUT, Constants.DEFAULT_START_TIMEOUT);
    }

}
