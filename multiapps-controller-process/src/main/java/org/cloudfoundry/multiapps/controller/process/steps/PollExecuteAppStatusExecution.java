package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.ApplicationLog;
import com.sap.cloudfoundry.client.facade.domain.ApplicationLog.MessageType;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientFactory;
import org.cloudfoundry.multiapps.controller.core.cf.apps.ApplicationStateAction;
import org.cloudfoundry.multiapps.controller.core.helpers.ApplicationAttributes;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLoggerProvider;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.text.MessageFormat.format;

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

    private final CloudControllerClientFactory clientFactory;
    private final TokenService tokenService;

    private static final String DEFAULT_SUCCESS_MARKER = "STDOUT:SUCCESS";
    private static final String DEFAULT_FAILURE_MARKER = "STDERR:FAILURE";

    public PollExecuteAppStatusExecution(CloudControllerClientFactory clientFactory, TokenService tokenService) {
        this.clientFactory = clientFactory;
        this.tokenService = tokenService;
    }

    @Override
    public AsyncExecutionState execute(ProcessContext context) {
        List<ApplicationStateAction> actions = context.getVariable(Variables.APP_STATE_ACTIONS_TO_EXECUTE);
        if (!actions.contains(ApplicationStateAction.EXECUTE)) {
            return AsyncExecutionState.FINISHED;
        }

        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
        CloudControllerClient client = context.getControllerClient();
        ApplicationAttributes appAttributes = ApplicationAttributes.fromApplication(app, app.getEnv());

        LocalDateTime logsOffset = context.getVariable(Variables.LOGS_OFFSET_FOR_APP_EXECUTION);
        var user = context.getVariable(Variables.USER);
        var userGuid = context.getVariable(Variables.USER_GUID);
        var correlationId = context.getVariable(Variables.CORRELATION_ID);
        var logCacheClient = clientFactory.createLogCacheClient(tokenService.getToken(user, userGuid), correlationId);

        UUID appGuid = client.getApplicationGuid(app.getName());
        List<ApplicationLog> recentLogs = logCacheClient.getRecentLogs(appGuid, logsOffset);
        setLogsOffset(context, recentLogs);

        AppExecutionDetailedStatus status = getAppExecutionStatus(context, appAttributes, recentLogs);
        ProcessLoggerProvider processLoggerProvider = context.getStepLogger()
                                                             .getProcessLoggerProvider();
        StepsUtil.saveAppLogs(context, logCacheClient, appGuid, app.getName(), LOGGER, processLoggerProvider);
        return checkAppExecutionStatus(context, app, appAttributes, status);
    }

    @Override
    public String getPollingErrorMessage(ProcessContext context) {
        CloudApplication app = context.getVariable(Variables.APP_TO_PROCESS);
        return MessageFormat.format(Messages.ERROR_EXECUTING_APP_1, app.getName());
    }

    private AppExecutionDetailedStatus getAppExecutionStatus(ProcessContext context, ApplicationAttributes appAttributes,
                                                             List<ApplicationLog> recentLogs) {
        long startTime = context.getVariable(Variables.START_TIME);
        Marker successMarker = getMarker(appAttributes, SupportedParameters.SUCCESS_MARKER, DEFAULT_SUCCESS_MARKER);
        Marker failureMarker = getMarker(appAttributes, SupportedParameters.FAILURE_MARKER, DEFAULT_FAILURE_MARKER);
        boolean checkDeployId = appAttributes.get(SupportedParameters.CHECK_DEPLOY_ID, Boolean.class, Boolean.FALSE);
        String deployId = checkDeployId ? (StepsUtil.DEPLOY_ID_PREFIX + context.getVariable(Variables.CORRELATION_ID)) : null;

        return recentLogs.stream()
                         .filter(this::isLogFromApp)
                         .filter(log -> isAfterStartTime(log, startTime))
                         .filter(log -> deployId == null || log.getMessage()
                                                               .contains(deployId))
                         .map(log -> getAppExecutionStatus(log, successMarker, failureMarker))
                         .filter(Objects::nonNull)
                         .findFirst()
                         .orElse(new AppExecutionDetailedStatus(AppExecutionStatus.EXECUTING));
    }

    private void setLogsOffset(ProcessContext context, List<ApplicationLog> recentLogs) {
        if (recentLogs.isEmpty()) {
            return;
        }
        var lastLog = recentLogs.get(recentLogs.size() - 1);
        context.setVariable(Variables.LOGS_OFFSET_FOR_APP_EXECUTION, lastLog.getTimestamp());
    }

    private boolean isLogFromApp(ApplicationLog log) {
        return log.getSourceName()
                  .toUpperCase()
                  .startsWith("APP");
    }

    private boolean isAfterStartTime(ApplicationLog log, long startTime) {
        return log.getTimestamp()
                  .toInstant(ZoneOffset.UTC)
                  .compareTo(Instant.ofEpochMilli(startTime)) >= 0;
    }

    private AppExecutionDetailedStatus getAppExecutionStatus(ApplicationLog log, Marker successMarker, Marker failureMarker) {
        MessageType messageType = log.getMessageType();
        String message = log.getMessage()
                            .trim();
        if (messageType.equals(successMarker.messageType) && message.matches(successMarker.text)) {
            return new AppExecutionDetailedStatus(AppExecutionStatus.SUCCEEDED);
        }
        if (messageType.equals(failureMarker.messageType) && message.matches(failureMarker.text)) {
            return new AppExecutionDetailedStatus(AppExecutionStatus.FAILED, message);
        }
        return null;
    }

    private AsyncExecutionState checkAppExecutionStatus(ProcessContext context, CloudApplication app,
                                                        ApplicationAttributes appAttributes, AppExecutionDetailedStatus status) {
        var execStatus = status.getStatus();
        boolean stopApp = appAttributes.get(SupportedParameters.STOP_APP, Boolean.class, Boolean.FALSE);

        if (execStatus == AppExecutionStatus.FAILED) {
            context.getStepLogger()
                   .error(format(Messages.ERROR_EXECUTING_APP_2, app.getName(), status.getMessage()));
            if (stopApp) {
                stopApplication(context, app);
            }
            return AsyncExecutionState.ERROR;
        }
        if (execStatus == AppExecutionStatus.SUCCEEDED) {
            context.getStepLogger()
                   .info(Messages.APP_EXECUTED, app.getName());
            if (stopApp) {
                stopApplication(context, app);
            }
            return AsyncExecutionState.FINISHED;
        }
        return AsyncExecutionState.RUNNING;
    }

    private void stopApplication(ProcessContext context, CloudApplication app) {
        context.getStepLogger()
               .info(Messages.STOPPING_APP, app.getName());
        context.getControllerClient()
               .stopApplication(app.getName());
        context.getStepLogger()
               .debug(Messages.APP_STOPPED, app.getName());
    }

    private static Marker getMarker(ApplicationAttributes appAttributes, String attributeName, String defaultValue) {
        MessageType messageType;
        String text;
        String attributeValue = appAttributes.get(attributeName, String.class, defaultValue);
        if (attributeValue.startsWith(MessageType.STDERR + ":")) {
            messageType = MessageType.STDERR;
            text = attributeValue.substring(MessageType.STDERR.toString()
                                                              .length() + 1);
        } else if (attributeValue.startsWith(MessageType.STDOUT + ":")) {
            messageType = MessageType.STDOUT;
            text = attributeValue.substring(MessageType.STDOUT.toString()
                                                              .length() + 1);
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
