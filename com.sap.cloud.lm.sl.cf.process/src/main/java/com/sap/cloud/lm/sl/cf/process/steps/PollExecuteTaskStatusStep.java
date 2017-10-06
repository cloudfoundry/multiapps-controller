package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudTask;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Component("pollExecuteTaskStatusStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PollExecuteTaskStatusStep extends AbstractXS2ProcessStepWithBridge {

    protected Supplier<Long> currentTimeSupplier = () -> System.currentTimeMillis();

    @Override
    public String getLogicalStepName() {
        return ExecuteTaskStep.class.getSimpleName();
    }

    @Override
    protected String getIndexVariable() {
        return Constants.VAR_TASKS_INDEX;
    }

    @Override
    protected ExecutionStatus pollStatusInternal(DelegateExecution context) throws Exception {
        getStepLogger().logActivitiTask();

        CloudApplicationExtended app = StepsUtil.getApp(context);
        CloudTask task = StepsUtil.getStartedTask(context);
        try {
            return new PollExecuteTaskStatusDelegate(context).execute();
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            getStepLogger().error(e, Messages.ERROR_EXECUTING_TASK_ON_APP, task.getName(), app.getName());
            throw e;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_EXECUTING_TASK_ON_APP, task.getName(), app.getName());
            throw e;
        }
    }

    public class PollExecuteTaskStatusDelegate {

        private DelegateExecution context;
        private CloudApplicationExtended app;
        private CloudTask taskToPoll;

        public PollExecuteTaskStatusDelegate(DelegateExecution context) {
            this.taskToPoll = StepsUtil.getStartedTask(context);
            this.context = context;
            this.app = StepsUtil.getApp(context);
        }

        public ExecutionStatus execute() {
            CloudTask.State currentState = getCurrentState();
            reportCurrentState(currentState);
            return handleCurrentState(currentState);
        }

        private CloudTask.State getCurrentState() {
            ClientExtensions clientExtensions = getClientExtensions(context);
            List<CloudTask> allTasksForApp = clientExtensions.getTasks(app.getName());

            return findTaskWithGuid(allTasksForApp, taskToPoll.getMeta().getGuid()).getState();
        }

        private CloudTask findTaskWithGuid(List<CloudTask> allTasksForApp, UUID guid) {
            return allTasksForApp.stream().filter(task -> task.getMeta().getGuid().equals(guid)).findAny().orElseThrow(
                () -> new IllegalStateException(format(Messages.COULD_NOT_FIND_TASK_WITH_GUID, guid)));
        }

        private void reportCurrentState(CloudTask.State currentState) {
            getStepLogger().info(Messages.TASK_EXECUTION_STATUS, currentState.toString().toLowerCase());
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
                getStepLogger().error(Messages.ERROR_EXECUTING_TASK_ON_APP, taskToPoll.getName(), app.getName());
                return ExecutionStatus.LOGICAL_RETRY;
            }
            return ExecutionStatus.SUCCESS;
        }

        private ExecutionStatus checkTimeout() {
            if (StepsUtil.hasTimedOut(context, currentTimeSupplier)) {
                String message = format(Messages.EXECUTING_TASK_ON_APP_TIMED_OUT, taskToPoll.getName(), app.getName());
                getStepLogger().error(message);
                setRetryMessage(context, message);
                return ExecutionStatus.LOGICAL_RETRY;
            }
            return ExecutionStatus.RUNNING;
        }
    }

}
