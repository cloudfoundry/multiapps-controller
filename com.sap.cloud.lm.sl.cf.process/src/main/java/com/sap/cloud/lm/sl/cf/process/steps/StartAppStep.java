package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Component("startAppStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class StartAppStep extends AbstractProcessStep {

    @Override
    protected String getIndexVariable() {
        return Constants.VAR_APPS_INDEX;
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) {
        getStepLogger().logActivitiTask();

        CloudApplication app = getAppToStart(context);
        try {
            attemptToStartApp(context, app);
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            onError(format(Messages.ERROR_STARTING_APP_1, app.getName()), e);
            throw e;
        }
        return ExecutionStatus.SUCCESS;
    }

    protected void onError(String message, Exception e) {
        getStepLogger().error(e, message);
    }

    private void attemptToStartApp(DelegateExecution context, CloudApplication app) {
        CloudFoundryOperations client = getCloudFoundryClient(context);

        if (isAppStarted(client, app.getName())) {
            stopApp(client, app);
        }
        StartingInfo startingInfo = startApp(context, client, app);
        StepsUtil.setStartingInfo(context, startingInfo);
        if (context.getVariable(Constants.VAR_START_TIME) == null) {
            context.setVariable(Constants.VAR_START_TIME, System.currentTimeMillis());
        }
        if (context.getVariable(Constants.VAR_OFFSET) == null) {
            context.setVariable(Constants.VAR_OFFSET, 0);
        }
    }

    protected CloudApplication getAppToStart(DelegateExecution context) {
        return StepsUtil.getApp(context);
    }

    private boolean isAppStarted(CloudFoundryOperations client, String appName) {
        try {
            CloudApplication app2 = client.getApplication(appName);
            return app2.getState().equals(AppState.STARTED);
        } catch (CloudFoundryException e) {
            if (e.getStatusCode().equals(HttpStatus.INTERNAL_SERVER_ERROR)) {
                LOGGER.getLoggerImpl().warn(e.getMessage(), e);
                return false;
            }
            throw e;
        }
    }

    private void stopApp(CloudFoundryOperations client, CloudApplication app) {
        getStepLogger().info(Messages.STOPPING_APP, app.getName());
        client.stopApplication(app.getName());
    }

    private StartingInfo startApp(DelegateExecution context, CloudFoundryOperations client, CloudApplication app) {
        ClientExtensions clientExtensions = getClientExtensions(context);
        getStepLogger().info(Messages.STARTING_APP, app.getName());
        if (clientExtensions != null) {
            return clientExtensions.startApplication(app.getName(), false);
        }
        return client.startApplication(app.getName());
    }

}
