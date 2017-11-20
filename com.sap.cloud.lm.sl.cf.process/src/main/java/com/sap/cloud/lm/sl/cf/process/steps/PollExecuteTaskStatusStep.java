package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudTask;
import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

public class PollExecuteTaskStatusStep extends AsyncStepOperation {

    protected Supplier<Long> currentTimeSupplier;

    private RecentLogsRetriever recentLogsRetriever;

    public PollExecuteTaskStatusStep(RecentLogsRetriever recentLogsRetriever, Supplier<Long> currentTimeSupplier) {
        this.recentLogsRetriever = recentLogsRetriever;
        this.currentTimeSupplier = currentTimeSupplier;
    }

    @Override
    public ExecutionStatus executeOperation(ExecutionWrapper execution) throws Exception {
        execution.getStepLogger().logActivitiTask();

        CloudApplicationExtended app = StepsUtil.getApp(execution.getContext());
        CloudTask task = StepsUtil.getStartedTask(execution.getContext());
        try {
            return new PollExecuteTaskStatusDelegate(execution).execute();
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            execution.getStepLogger().error(e, Messages.ERROR_EXECUTING_TASK_ON_APP, task.getName(), app.getName());
            throw e;
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

        public ExecutionStatus execute() {
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
            StepsUtil.saveAppLogs(execution.getContext(), client, recentLogsRetriever, app, LOGGER.getLoggerImpl(),
                execution.getProcessLoggerProviderFactory());
        }

        private ExecutionStatus handleCurrentState(CloudTask.State currentState) {
            if (isFinalState(currentState)) {
                return handleFinalState(currentState);
            }
            return checkTimeout();
        }

        private boolean isFinalState(CloudTask.State currentState) {
            return currentState.equals(CloudTask.State.FAILED) || currentState.equals(CloudTask.State.SUCCEEDED);
        }

        private ExecutionStatus handleFinalState(CloudTask.State state) {
            if (state.equals(CloudTask.State.FAILED)) {
                execution.getStepLogger().error(Messages.ERROR_EXECUTING_TASK_ON_APP, taskToPoll.getName(), app.getName());
                StepsUtil.setStepPhase(execution, StepPhase.RETRY);
                return ExecutionStatus.FAILED;
            }
            StepsUtil.setStepPhase(execution, StepPhase.EXECUTE);
            return ExecutionStatus.SUCCESS;
        }

        private ExecutionStatus checkTimeout() {
            if (StepsUtil.hasTimedOut(execution.getContext(), currentTimeSupplier)) {
                String message = format(Messages.EXECUTING_TASK_ON_APP_TIMED_OUT, taskToPoll.getName(), app.getName());
                execution.getStepLogger().error(message);
                StepsUtil.setStepPhase(execution, StepPhase.RETRY);
                return ExecutionStatus.FAILED;
            }
            StepsUtil.setStepPhase(execution, StepPhase.POLL);
            return ExecutionStatus.RUNNING;
        }
    }

}
