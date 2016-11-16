package com.sap.cloud.lm.sl.cf.process.steps;

import static com.sap.cloud.lm.sl.cf.process.steps.StepsUtil.appLog;
import static java.text.MessageFormat.format;

import java.util.function.Function;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.ApplicationLogListener;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.StreamingLogToken;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.activiti.common.util.ContextUtil;
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.AsyncStepMetadata;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("startAppStep")
public class StartAppStep extends AbstractXS2ProcessStepWithBridge {

    // Logger
    private static final Logger LOGGER = LoggerFactory.getLogger(StartAppStep.class);

    public static StepMetadata getMetadata() {
        return new AsyncStepMetadata("startAppTask", "Start App", "Start App", "pollStartAppStatusTask", "pollStartAppStatusTimer");
    }

    protected Function<DelegateExecution, CloudFoundryOperations> clientSupplier = (context) -> getCloudFoundryClient(context, LOGGER);

    protected Function<DelegateExecution, ClientExtensions> extensionsSupplier = (context) -> getClientExtensions(context, LOGGER);

    private static final String OPERATION_ID = "startApp";

    @Override
    protected ExecutionStatus pollStatusInternal(DelegateExecution context) throws SLException {

        logActivitiTask(context, LOGGER);

        // Get the next cloud application from the context
        CloudApplication app = StepsUtil.getApp(context);

        try {
            info(context, format(Messages.STARTING_APP, app.getName()), LOGGER);

            // Get a cloud foundry client
            CloudFoundryOperations client = clientSupplier.apply(context);

            // Stop the application if already started
            context.setVariable(Constants.VAR_START_TIME, System.currentTimeMillis());
            if (isAppStarted(client, app.getName())) {
                client.stopApplication(app.getName());
            }

            // If client supports extensions (XS2), initiate staging
            // Otherwise (CF), initiate startup
            StartingInfo startingInfo;
            ClientExtensions clientExtensions = extensionsSupplier.apply(context);
            if (clientExtensions != null) {
                startingInfo = clientExtensions.stageApplication(app.getName());
            } else {
                startingInfo = client.startApplication(app.getName());
            }

            // Based on the availability of staging logs initialize the phase and other params
            if (startingInfo != null) {
                // Initiating the operation was successful
                context.setVariable(Constants.VAR_START_PHASE, Constants.START_PHASE_STAGING);
                StepsUtil.setStartingInfo(context, startingInfo);
                context.setVariable(Constants.VAR_OFFSET, 0);
            } else {
                // Initiating the operation failed, assume startup
                context.setVariable(Constants.VAR_START_PHASE, Constants.START_PHASE_STARTUP);
            }

            // TODO: Does this code work well at all Activiti?
            boolean streamAppLogs = ContextUtil.getVariable(context, Constants.PARAM_STREAM_APP_LOGS, false);
            if (streamAppLogs) {
                // Stream application logs
                StreamingLogToken streamingLogToken = client.streamLogs(app.getName(),
                    new StatusApplicationLogListener(context, app.getName()));
                if (streamingLogToken != null) {
                    StepsUtil.setStreamingLogsToken(context, streamingLogToken);
                }
            }

            getBridge().setOperationId(context, OPERATION_ID);

            return ExecutionStatus.SUCCESS;
        } catch (SLException e) {
            error(context, format(Messages.ERROR_STARTING_APP, app.getName()), e, LOGGER);
            throw e;
        } catch (CloudFoundryException e) {
            SLException ex = StepsUtil.createException(e);
            error(context, format(Messages.ERROR_STARTING_APP, app.getName()), ex, LOGGER);
            throw ex;
        }
    }

    private boolean isAppStarted(CloudFoundryOperations client, String appName) {
        boolean appStarted = false;
        try {
            CloudApplication app2 = client.getApplication(appName);
            appStarted = app2.getState().equals(AppState.STARTED);
        } catch (CloudFoundryException e) {
            if (!e.getStatusCode().equals(HttpStatus.INTERNAL_SERVER_ERROR)) {
                throw e;
            }
        }
        return appStarted;
    }

    private final class StatusApplicationLogListener implements ApplicationLogListener {

        private final DelegateExecution context;
        private final String appName;

        public StatusApplicationLogListener(DelegateExecution context, String appName) {
            this.context = context;
            this.appName = appName;
        }

        @Override
        public void onMessage(ApplicationLog log) {
            appLog(context, appName, log.toString(), LOGGER, processLoggerProviderFactory);
        }

        @Override
        public void onComplete() {
            appLog(context, appName, "Logging complete", LOGGER, processLoggerProviderFactory);
        }

        @Override
        public void onError(Throwable exception) {
            appLog(context, appName, "Error: " + exception.getMessage(), LOGGER, processLoggerProviderFactory);
        }
    }
}
