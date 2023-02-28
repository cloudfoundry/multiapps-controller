package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLoggerProvider;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudTask;

public class PollExecuteTaskStatusExecution implements AsyncExecution {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollExecuteTaskStatusExecution.class);

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
        StepsUtil.saveAppLogs(context, client, app.getName(), LOGGER, processLoggerProvider);

        if (currentState == CloudTask.State.SUCCEEDED) {
            return AsyncExecutionState.FINISHED;
        }
        if (currentState == CloudTask.State.FAILED) {
            context.getStepLogger()
                   .error(Messages.ERROR_EXECUTING_TASK_0_ON_APP_1, taskToPoll.getName(), app.getName());
            return AsyncExecutionState.ERROR;
        }
        return AsyncExecutionState.RUNNING;
    }

    @Override
    public String getPollingErrorMessage(ProcessContext context) {
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
        CloudTask task = context.getVariable(Variables.STARTED_TASK);
        return MessageFormat.format(Messages.ERROR_EXECUTING_TASK_0_ON_APP_1, task.getName(), app.getName());
    }
}
