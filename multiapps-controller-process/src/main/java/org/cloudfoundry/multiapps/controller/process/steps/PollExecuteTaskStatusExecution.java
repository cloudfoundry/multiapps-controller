package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudTask;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientFactory;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLoggerProvider;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PollExecuteTaskStatusExecution implements AsyncExecution {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollExecuteTaskStatusExecution.class);

    private static final long LOG_INTERVAL_FOR_MESSAGE = Duration.ofMinutes(Constants.LOG_STALLED_TASK_MINUTE_INTERVAL)
                                                                 .toMillis();

    private final CloudControllerClientFactory clientFactory;
    private final TokenService tokenService;

    public PollExecuteTaskStatusExecution(CloudControllerClientFactory clientFactory, TokenService tokenService) {
        this.clientFactory = clientFactory;
        this.tokenService = tokenService;
    }

    @Override
    public AsyncExecutionState execute(ProcessContext context) {
        CloudTask taskToPoll = context.getVariable(Variables.STARTED_TASK);
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
        CloudControllerClient client = context.getControllerClient();

        CloudTask.State currentState = client.getTask(taskToPoll.getGuid())
                                             .getState();
        context.getStepLogger()
               .debug(Messages.TASK_EXECUTION_STATUS, currentState.toString()
                                                                  .toLowerCase());
        ProcessLoggerProvider processLoggerProvider = context.getStepLogger()
                                                             .getProcessLoggerProvider();

        var userGuid = context.getVariable(Variables.USER_GUID);
        var correlationId = context.getVariable(Variables.CORRELATION_ID);
        var logCacheClient = clientFactory.createLogCacheClient(tokenService.getToken(userGuid), correlationId);

        UUID appGuid = client.getApplicationGuid(app.getName());
        StepsUtil.saveAppLogs(context, logCacheClient, appGuid, app.getName(), LOGGER, processLoggerProvider);

        if (currentState == CloudTask.State.SUCCEEDED) {
            return AsyncExecutionState.FINISHED;
        }
        if (currentState == CloudTask.State.FAILED) {
            context.getStepLogger()
                   .error(Messages.ERROR_EXECUTING_TASK_0_ON_APP_1, taskToPoll.getName(), app.getName());
            context.setVariable(Variables.LAST_TASK_POLL_LOG_TIMESTAMP, Constants.UNSET_LAST_LOG_TIMESTAMP_MS);
            return AsyncExecutionState.ERROR;
        }
        if (currentState == CloudTask.State.RUNNING || currentState == CloudTask.State.PENDING) {
            logStalledTask(context, taskToPoll, app, currentState);
        }
        return AsyncExecutionState.RUNNING;
    }

    @Override
    public String getPollingErrorMessage(ProcessContext context) {
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
        CloudTask task = context.getVariable(Variables.STARTED_TASK);
        return MessageFormat.format(Messages.ERROR_EXECUTING_TASK_0_ON_APP_1, task.getName(), app.getName());
    }

    private void logStalledTask(ProcessContext context, CloudTask task, CloudApplicationExtended app, CloudTask.State currentState) {
        Long currentTimeNow = System.currentTimeMillis();
        Long lastLog = context.getVariable(Variables.LAST_TASK_POLL_LOG_TIMESTAMP);
        if (!lastLog.equals(Constants.UNSET_LAST_LOG_TIMESTAMP_MS)) {
            printTaskExecutionStatusIfDue(context, task, app, currentState, currentTimeNow, lastLog);
        } else {
            context.setVariable(Variables.LAST_TASK_POLL_LOG_TIMESTAMP, currentTimeNow);
        }
    }

    private void printTaskExecutionStatusIfDue(ProcessContext context, CloudTask task, CloudApplicationExtended app,
                                               CloudTask.State currentState, Long currentTimeNow, Long lastLog) {
        if (currentTimeNow - lastLog >= LOG_INTERVAL_FOR_MESSAGE) {
            context.getStepLogger()
                   .info(Messages.TASK_0_ON_APPLICATION_1_IS_STILL_2, task.getName(), app.getName(), currentState.name()
                                                                                                                 .toLowerCase());
            context.setVariable(Variables.LAST_TASK_POLL_LOG_TIMESTAMP, currentTimeNow);
        }
    }

}
