package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.cf.clients.RecentLogsRetriever;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLoggerProvider;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudTask;

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
            return client.getTask(taskToPoll.getGuid())
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
            StepsUtil.saveAppLogs(context, client, recentLogsRetriever, app.getName(), LOGGER, processLoggerProvider);
        }

        private AsyncExecutionState handleCurrentState(CloudTask.State currentState) {
            switch (currentState) {
                case SUCCEEDED:
                    return AsyncExecutionState.FINISHED;
                case FAILED:
                    context.getStepLogger()
                           .error(Messages.ERROR_EXECUTING_TASK_0_ON_APP_1, taskToPoll.getName(), app.getName());
                    return AsyncExecutionState.ERROR;
                default:
                    return AsyncExecutionState.RUNNING;
            }
        }

    }
}
