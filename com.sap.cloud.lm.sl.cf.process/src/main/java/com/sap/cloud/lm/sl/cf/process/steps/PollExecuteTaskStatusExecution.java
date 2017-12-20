package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;

import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudTask;
import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

public class PollExecuteTaskStatusExecution extends AsyncExecution {

    protected Supplier<Long> currentTimeSupplier;

    private RecentLogsRetriever recentLogsRetriever;

    public PollExecuteTaskStatusExecution(RecentLogsRetriever recentLogsRetriever, Supplier<Long> currentTimeSupplier) {
        this.recentLogsRetriever = recentLogsRetriever;
        this.currentTimeSupplier = currentTimeSupplier;
    }

    @Override
    public AsyncExecutionState execute(ExecutionWrapper execution) throws Exception {
        execution.getStepLogger().logActivitiTask();

        CloudApplicationExtended app = StepsUtil.getApp(execution.getContext());
        CloudTask task = StepsUtil.getStartedTask(execution.getContext());
        try {
            return new PollExecuteTaskStatusDelegate(execution).execute();
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            execution.getStepLogger().error(e, Messages.ERROR_EXECUTING_TASK_ON_APP, task.getName(), app.getName());
            throw cfe;
        } catch (SLException e) {
            execution.getStepLogger().error(e, Messages.ERROR_EXECUTING_TASK_ON_APP, task.getName(), app.getName());
            throw e;
        }
    }

    public class PollExecuteTaskStatusDelegate {

        private ExecutionWrapper execution;
        private CloudApplicationExtended app;
        private CloudTask taskToPoll;

        public PollExecuteTaskStatusDelegate(ExecutionWrapper execution) {
            this.taskToPoll = StepsUtil.getStartedTask(execution.getContext());
            this.execution = execution;
            this.app = StepsUtil.getApp(execution.getContext());
        }

        public AsyncExecutionState execute() {
            CloudTask.State currentState = getCurrentState();
            reportCurrentState(currentState);
            saveAppLogs();
            return handleCurrentState(currentState);
        }

        private CloudTask.State getCurrentState() {
            ClientExtensions clientExtensions = execution.getClientExtensions();
            List<CloudTask> allTasksForApp = clientExtensions.getTasks(app.getName());

            return findTaskWithGuid(allTasksForApp, taskToPoll.getMeta().getGuid()).getState();
        }

        private CloudTask findTaskWithGuid(List<CloudTask> allTasksForApp, UUID guid) {
            return allTasksForApp.stream().filter(task -> task.getMeta().getGuid().equals(guid)).findAny().orElseThrow(
                () -> new IllegalStateException(format(Messages.COULD_NOT_FIND_TASK_WITH_GUID, guid)));
        }

        private void reportCurrentState(CloudTask.State currentState) {
            execution.getStepLogger().info(Messages.TASK_EXECUTION_STATUS, currentState.toString().toLowerCase());
        }

        private void saveAppLogs() {
            CloudFoundryOperations client = execution.getCloudFoundryClient();
            StepsUtil.saveAppLogs(execution.getContext(), client, recentLogsRetriever, app, LOGGER,
                execution.getProcessLoggerProviderFactory());
        }

        private AsyncExecutionState handleCurrentState(CloudTask.State currentState) {
            if (isFinalState(currentState)) {
                return handleFinalState(currentState);
            }
            return checkTimeout();
        }

        private boolean isFinalState(CloudTask.State currentState) {
            return currentState.equals(CloudTask.State.FAILED) || currentState.equals(CloudTask.State.SUCCEEDED);
        }

        private AsyncExecutionState handleFinalState(CloudTask.State state) {
            if (state.equals(CloudTask.State.FAILED)) {
                execution.getStepLogger().error(Messages.ERROR_EXECUTING_TASK_ON_APP, taskToPoll.getName(), app.getName());
                return AsyncExecutionState.ERROR;
            }
            return AsyncExecutionState.FINISHED;
        }

        private AsyncExecutionState checkTimeout() {
            // if (StepsUtil.hasTimedOut(execution.getContext(), currentTimeSupplier)) {
            // String message = format(Messages.EXECUTING_TASK_ON_APP_TIMED_OUT, taskToPoll.getName(), app.getName());
            // execution.getStepLogger().error(message);
            // return AsyncExecutionState.ERROR;
            // }
            return AsyncExecutionState.RUNNING;
        }
    }

}
