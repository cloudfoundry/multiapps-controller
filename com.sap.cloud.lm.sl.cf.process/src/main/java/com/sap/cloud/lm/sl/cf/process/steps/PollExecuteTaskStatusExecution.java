package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLoggerProvider;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

public class PollExecuteTaskStatusExecution implements AsyncExecution {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollExecuteTaskStatusExecution.class);

    private final RecentLogsRetriever recentLogsRetriever;

    public PollExecuteTaskStatusExecution(RecentLogsRetriever recentLogsRetriever) {
        this.recentLogsRetriever = recentLogsRetriever;
    }

    @Override
    public AsyncExecutionState execute(ProcessContext context) {
        return new PollExecuteTaskStatusDelegate(context).execute();
    }

    public String getPollingErrorMessage(ProcessContext context) {
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
        CloudTask task = context.getVariable(Variables.STARTED_TASK);
        return MessageFormat.format(Messages.ERROR_EXECUTING_TASK_0_ON_APP_1, task.getName(), app.getName());
    }

    public class PollExecuteTaskStatusDelegate {

        private final ProcessContext context;
        private final CloudApplicationExtended app;
        private final CloudTask taskToPoll;

        public PollExecuteTaskStatusDelegate(ProcessContext context) {
            this.taskToPoll = context.getVariable(Variables.STARTED_TASK);
            this.context = context;
            this.app = context.getVariable(Variables.APP_TO_PROCESS);
        }

        public AsyncExecutionState execute() {
            CloudTask.State currentState = getCurrentState();
            reportCurrentState(currentState);
            saveAppLogs();
            return handleCurrentState(currentState);
        }

        private CloudTask.State getCurrentState() {
            CloudControllerClient client = context.getControllerClient();
            return client.getTask(taskToPoll.getMetadata()
                                            .getGuid())
                         .getState();
        }

        private void reportCurrentState(CloudTask.State currentState) {
            context.getStepLogger()
                   .debug(Messages.TASK_EXECUTION_STATUS, currentState.toString()
                                                                      .toLowerCase());
        }

        private void saveAppLogs() {
            CloudControllerClient client = context.getControllerClient();
            ProcessLoggerProvider processLoggerProvider = context.getStepLogger()
                                                                 .getProcessLoggerProvider();
            StepsUtil.saveAppLogs(context.getExecution(), client, recentLogsRetriever, app, LOGGER, processLoggerProvider);
        }

        private AsyncExecutionState handleCurrentState(CloudTask.State currentState) {
            if (isFinalState(currentState)) {
                return handleFinalState(currentState);
            }
            return AsyncExecutionState.RUNNING;
        }

        private boolean isFinalState(CloudTask.State currentState) {
            return currentState.equals(CloudTask.State.FAILED) || currentState.equals(CloudTask.State.SUCCEEDED);
        }

        private AsyncExecutionState handleFinalState(CloudTask.State state) {
            if (state.equals(CloudTask.State.FAILED)) {
                context.getStepLogger()
                       .error(Messages.ERROR_EXECUTING_TASK_0_ON_APP_1, taskToPoll.getName(), app.getName());
                return AsyncExecutionState.ERROR;
            }
            return AsyncExecutionState.FINISHED;
        }

    }
}
