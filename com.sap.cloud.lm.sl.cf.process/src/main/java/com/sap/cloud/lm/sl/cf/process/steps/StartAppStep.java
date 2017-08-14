package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("startAppStep")
public class StartAppStep extends AbstractXS2ProcessStepWithBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartAppStep.class);

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("startAppTask").displayName("Start App").description("Start App").children(
            PollStageAppStatusOnCfStep.getMetadata(), PollStartAppStatusStep.getMetadata(), PollExecuteAppStatusStep.getMetadata()).build();
    }

    @Override
    protected String getIndexVariable() {
        return Constants.VAR_APPS_INDEX;
    }

    @Override
    protected ExecutionStatus pollStatusInternal(DelegateExecution context) {
        logActivitiTask(context, LOGGER);

        CloudApplication app = getAppToStart(context);
        try {
            attemptToStartApp(context, app);
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            onError(context, format(Messages.ERROR_STARTING_APP_1, app.getName()), e);
            throw e;
        }
        return ExecutionStatus.SUCCESS;
    }

    protected void onError(DelegateExecution context, String message, Exception e) {
        error(context, message, e, LOGGER);
    }

    private void attemptToStartApp(DelegateExecution context, CloudApplication app) {
        CloudFoundryOperations client = getCloudFoundryClient(context, LOGGER);

        if (isAppStarted(client, app.getName())) {
            stopApp(context, client, app);
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
                LOGGER.warn(e.getMessage(), e);
                return false;
            }
            throw e;
        }
    }

    private void stopApp(DelegateExecution context, CloudFoundryOperations client, CloudApplication app) {
        info(context, format(Messages.STOPPING_APP, app.getName()), LOGGER);
        client.stopApplication(app.getName());
    }

    private StartingInfo startApp(DelegateExecution context, CloudFoundryOperations client, CloudApplication app) {
        ClientExtensions clientExtensions = getClientExtensions(context, LOGGER);
        info(context, format(Messages.STARTING_APP, app.getName()), LOGGER);
        if (clientExtensions != null) {
            return clientExtensions.startApplication(app.getName(), false);
        }
        return client.startApplication(app.getName());
    }

}
