package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.ApplicationLog.MessageType;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.flowable.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.cf.apps.ApplicationStateAction;
import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationAttributes;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLoggerProvider;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

public class PollExecuteAppStatusExecution implements AsyncExecution {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollExecuteAppStatusExecution.class);

    enum AppExecutionStatus {
        EXECUTING, SUCCEEDED, FAILED
    }

    private static class AppExecutionDetailedStatus {
        private final AppExecutionStatus status;
        private final String message;

        AppExecutionDetailedStatus(AppExecutionStatus status, String message) {
            this.status = status;
            this.message = message;
        }

        AppExecutionDetailedStatus(AppExecutionStatus status) {
            this(status, "");
        }

        AppExecutionStatus getStatus() {
            return status;
        }

        String getMessage() {
            return message;
        }
    }

    private static final String DEFAULT_SUCCESS_MARKER = "STDOUT:SUCCESS";
    private static final String DEFAULT_FAILURE_MARKER = "STDERR:FAILURE";

    private final RecentLogsRetriever recentLogsRetriever;

    public PollExecuteAppStatusExecution(RecentLogsRetriever recentLogsRetriever) {
        this.recentLogsRetriever = recentLogsRetriever;
    }

    @Override
    public AsyncExecutionState execute(ExecutionWrapper execution) {
        Set<ApplicationStateAction> actions = StepsUtil.getAppStateActionsToExecute(execution.getContext());
        if (!actions.contains(ApplicationStateAction.EXECUTE)) {
            return AsyncExecutionState.FINISHED;
        }

        CloudApplication app = getNextApp(execution);
        CloudControllerClient client = execution.getControllerClient();
        ApplicationAttributes appAttributes = ApplicationAttributes.fromApplication(app);
        AppExecutionDetailedStatus status = getAppExecutionStatus(execution.getContext(), client, appAttributes, app);
        ProcessLoggerProvider processLoggerProvider = execution.getStepLogger()
                                                               .getProcessLoggerProvider();
        StepsUtil.saveAppLogs(execution.getContext(), client, recentLogsRetriever, app, LOGGER, processLoggerProvider);
        return checkAppExecutionStatus(execution, client, app, appAttributes, status);

    }

    public String getPollingErrorMessage(ExecutionWrapper execution) {
        CloudApplication app = getNextApp(execution);
        return MessageFormat.format(Messages.ERROR_EXECUTING_APP_1, app.getName());
    }

    protected CloudApplication getNextApp(ExecutionWrapper execution) {
        return execution.getVariable(Variables.APP_TO_PROCESS);
    }

    private AppExecutionDetailedStatus getAppExecutionStatus(DelegateExecution context, CloudControllerClient client,
                                                             ApplicationAttributes appAttributes, CloudApplication app) {
        long startTime = (long) context.getVariable(Constants.VAR_START_TIME);
        Marker sm = getMarker(appAttributes, SupportedParameters.SUCCESS_MARKER, DEFAULT_SUCCESS_MARKER);
        Marker fm = getMarker(appAttributes, SupportedParameters.FAILURE_MARKER, DEFAULT_FAILURE_MARKER);
        boolean checkDeployId = appAttributes.get(SupportedParameters.CHECK_DEPLOY_ID, Boolean.class, Boolean.FALSE);
        String deployId = checkDeployId ? (StepsUtil.DEPLOY_ID_PREFIX + StepsUtil.getCorrelationId(context)) : null;

        List<ApplicationLog> recentLogs = recentLogsRetriever.getRecentLogs(client, app.getName(), null);
        return recentLogs.stream()
                         .map(log -> getAppExecutionStatus(log, startTime, sm, fm, deployId))
                         .filter(Objects::nonNull)
                         .reduce(new AppExecutionDetailedStatus(AppExecutionStatus.EXECUTING), (a, b) -> b);
    }

    private AppExecutionDetailedStatus getAppExecutionStatus(ApplicationLog log, long startTime, Marker sm, Marker fm, String id) {
        long time = log.getTimestamp()
                       .getTime();
        String sourceName = log.getSourceName()
                               .substring(0, 3);
        if (time < startTime || !sourceName.equalsIgnoreCase("APP"))
            return null;

        MessageType mt = log.getMessageType();
        String msg = log.getMessage()
                        .trim();
        if (!(id == null || msg.contains(id)))
            return null;

        if (mt.equals(sm.messageType) && msg.matches(sm.text)) {
            return new AppExecutionDetailedStatus(AppExecutionStatus.SUCCEEDED);
        } else if (mt.equals(fm.messageType) && msg.matches(fm.text)) {
            return new AppExecutionDetailedStatus(AppExecutionStatus.FAILED, msg);
        }
        return null;
    }

    private AsyncExecutionState checkAppExecutionStatus(ExecutionWrapper execution, CloudControllerClient client, CloudApplication app,
                                                        ApplicationAttributes appAttributes, AppExecutionDetailedStatus status) {
        if (status.getStatus()
                  .equals(AppExecutionStatus.FAILED)) {
            // Application execution failed
            String message = format(Messages.ERROR_EXECUTING_APP_2, app.getName(), status.getMessage());
            execution.getStepLogger()
                     .error(message);
            stopApplicationIfSpecified(execution, client, app, appAttributes);
            return AsyncExecutionState.ERROR;
        } else if (status.getStatus()
                         .equals(AppExecutionStatus.SUCCEEDED)) {
            // Application executed successfully
            execution.getStepLogger()
                     .info(Messages.APP_EXECUTED, app.getName());
            stopApplicationIfSpecified(execution, client, app, appAttributes);
            return AsyncExecutionState.FINISHED;
        } else {
            // Application not executed yet, wait and try again unless it's a timeout.
            return AsyncExecutionState.RUNNING;
        }
    }

    private void stopApplicationIfSpecified(ExecutionWrapper execution, CloudControllerClient client, CloudApplication app,
                                            ApplicationAttributes appAttributes) {
        boolean stopApp = appAttributes.get(SupportedParameters.STOP_APP, Boolean.class, Boolean.FALSE);
        if (!stopApp) {
            return;
        }
        execution.getStepLogger()
                 .info(Messages.STOPPING_APP, app.getName());
        client.stopApplication(app.getName());
        execution.getStepLogger()
                 .debug(Messages.APP_STOPPED, app.getName());
    }

    private static Marker getMarker(ApplicationAttributes appAttributes, String attribute, String defaultValue) {
        MessageType messageType;
        String text;
        String attr = appAttributes.get(attribute, String.class, defaultValue);
        if (attr.startsWith(MessageType.STDERR.toString() + ":")) {
            messageType = MessageType.STDERR;
            text = attr.substring(MessageType.STDERR.toString()
                                                    .length()
                + 1);
        } else if (attr.startsWith(MessageType.STDOUT.toString() + ":")) {
            messageType = MessageType.STDOUT;
            text = attr.substring(MessageType.STDOUT.toString()
                                                    .length()
                + 1);
        } else {
            messageType = MessageType.STDOUT;
            text = attr;
        }
        return new Marker(messageType, text);
    }

    private static class Marker {
        final MessageType messageType;
        final String text;

        Marker(MessageType messageType, String text) {
            this.messageType = messageType;
            this.text = text;
        }
    }
}
