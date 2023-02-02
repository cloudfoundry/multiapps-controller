package org.cloudfoundry.multiapps.controller.process.steps;

import static java.text.MessageFormat.format;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.cf.apps.ApplicationStateAction;
import org.cloudfoundry.multiapps.controller.core.cf.clients.RecentLogsRetriever;
import org.cloudfoundry.multiapps.controller.core.helpers.ApplicationAttributes;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.util.ImmutableLogsOffset;
import org.cloudfoundry.multiapps.controller.core.util.LogsOffset;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLoggerProvider;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.ApplicationLog;
import com.sap.cloudfoundry.client.facade.domain.ApplicationLog.MessageType;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;

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
    public AsyncExecutionState execute(ProcessContext context) {
        List<ApplicationStateAction> actions = context.getVariable(Variables.APP_STATE_ACTIONS_TO_EXECUTE);
        if (!actions.contains(ApplicationStateAction.EXECUTE)) {
            return AsyncExecutionState.FINISHED;
        }
        CloudApplicationExtended app = getNextApp(context);
        CloudControllerClient client = context.getControllerClient();
        ApplicationAttributes appAttributes = ApplicationAttributes.fromApplication(app, app.getEnv());
        AppExecutionDetailedStatus status = getAppExecutionStatus(context, client, appAttributes, app);
        ProcessLoggerProvider processLoggerProvider = context.getStepLogger()
                                                             .getProcessLoggerProvider();
        StepsUtil.saveAppLogs(context, client, recentLogsRetriever, app.getName(), LOGGER, processLoggerProvider);
        return checkAppExecutionStatus(context, client, app, appAttributes, status);
    }

    public String getPollingErrorMessage(ProcessContext context) {
        CloudApplication app = getNextApp(context);
        return MessageFormat.format(Messages.ERROR_EXECUTING_APP_1, app.getName());
    }

    protected CloudApplicationExtended getNextApp(ProcessContext context) {
        return context.getVariable(Variables.APP_TO_PROCESS);
    }

    private AppExecutionDetailedStatus getAppExecutionStatus(ProcessContext context, CloudControllerClient client,
                                                             ApplicationAttributes appAttributes, CloudApplication app) {
        long startTime = context.getVariable(Variables.START_TIME);
        Marker successMarker = getMarker(appAttributes, SupportedParameters.SUCCESS_MARKER, DEFAULT_SUCCESS_MARKER);
        Marker failureMarker = getMarker(appAttributes, SupportedParameters.FAILURE_MARKER, DEFAULT_FAILURE_MARKER);
        boolean checkDeployId = appAttributes.get(SupportedParameters.CHECK_DEPLOY_ID, Boolean.class, Boolean.FALSE);
        String deployId = checkDeployId ? (StepsUtil.DEPLOY_ID_PREFIX + context.getVariable(Variables.CORRELATION_ID)) : null;
        LogsOffset logsOffset = context.getVariable(Variables.LOGS_OFFSET_FOR_APP_EXECUTION);
        List<ApplicationLog> recentLogs = recentLogsRetriever.getRecentLogs(client, app.getName(), logsOffset);
        setLogsOffset(context, recentLogs);
        return recentLogs.stream()
                         .map(log -> getAppExecutionStatus(log, startTime, successMarker, failureMarker, deployId))
                         .filter(Optional::isPresent)
                         .map(Optional::get)
                         .reduce(new AppExecutionDetailedStatus(AppExecutionStatus.EXECUTING), (currentLog, nextLog) -> nextLog);
    }

    private void setLogsOffset(ProcessContext context, List<ApplicationLog> recentLogs) {
        if (recentLogs.isEmpty()) {
            return;
        }
        ApplicationLog lastLog = recentLogs.get(recentLogs.size() - 1);
        context.setVariable(Variables.LOGS_OFFSET_FOR_APP_EXECUTION, ImmutableLogsOffset.builder()
                                                                                        .message(lastLog.getMessage())
                                                                                        .timestamp(lastLog.getTimestamp())
                                                                                        .build());
    }

    private Optional<AppExecutionDetailedStatus> getAppExecutionStatus(ApplicationLog log, long startTime, Marker successMarker,
                                                                       Marker failureMarker, String deployId) {
        long time = log.getTimestamp()
                       .getTime();
        String sourceName = log.getSourceName()
                               .substring(0, 3);
        if (time < startTime || !sourceName.equalsIgnoreCase("APP")) {
            return Optional.empty();
        }
        MessageType messageType = log.getMessageType();
        String message = log.getMessage()
                            .trim();
        if (!(deployId == null || message.contains(deployId))) {
            return Optional.empty();
        }
        if (messageType.equals(successMarker.messageType) && message.matches(successMarker.text)) {
            return Optional.of(new AppExecutionDetailedStatus(AppExecutionStatus.SUCCEEDED));
        } else if (messageType.equals(failureMarker.messageType) && message.matches(failureMarker.text)) {
            return Optional.of(new AppExecutionDetailedStatus(AppExecutionStatus.FAILED, message));
        }
        return Optional.empty();
    }

    private AsyncExecutionState checkAppExecutionStatus(ProcessContext context, CloudControllerClient client, CloudApplication app,
                                                        ApplicationAttributes appAttributes, AppExecutionDetailedStatus status) {
        if (status.getStatus()
                  .equals(AppExecutionStatus.FAILED)) {
            String message = format(Messages.ERROR_EXECUTING_APP_2, app.getName(), status.getMessage());
            context.getStepLogger()
                   .error(message);
            stopApplicationIfSpecified(context, client, app, appAttributes);
            return AsyncExecutionState.ERROR;
        } else if (status.getStatus()
                         .equals(AppExecutionStatus.SUCCEEDED)) {
            context.getStepLogger()
                   .info(Messages.APP_EXECUTED, app.getName());
            stopApplicationIfSpecified(context, client, app, appAttributes);
            return AsyncExecutionState.FINISHED;
        }
        return AsyncExecutionState.RUNNING;
    }

    private void stopApplicationIfSpecified(ProcessContext context, CloudControllerClient client, CloudApplication app,
                                            ApplicationAttributes appAttributes) {
        boolean stopApp = appAttributes.get(SupportedParameters.STOP_APP, Boolean.class, Boolean.FALSE);
        if (!stopApp) {
            return;
        }
        context.getStepLogger()
               .info(Messages.STOPPING_APP, app.getName());
        client.stopApplication(app.getName());
        context.getStepLogger()
               .debug(Messages.APP_STOPPED, app.getName());
    }

    private Marker getMarker(ApplicationAttributes appAttributes, String attributeName, String defaultValue) {
        MessageType messageType;
        String text;
        String attributeValue = appAttributes.get(attributeName, String.class, defaultValue);
        if (attributeValue.startsWith(MessageType.STDERR + ":")) {
            messageType = MessageType.STDERR;
            text = attributeValue.substring(MessageType.STDERR.toString()
                                                              .length()
                + 1);
        } else if (attributeValue.startsWith(MessageType.STDOUT + ":")) {
            messageType = MessageType.STDOUT;
            text = attributeValue.substring(MessageType.STDOUT.toString()
                                                              .length()
                + 1);
        } else {
            messageType = MessageType.STDOUT;
            text = attributeValue;
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
