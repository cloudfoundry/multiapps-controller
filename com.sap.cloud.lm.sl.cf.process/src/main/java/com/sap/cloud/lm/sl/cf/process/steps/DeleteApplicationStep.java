package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.List;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudTask;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.process.message.Messages;

@Component("deleteApplicationStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteApplicationStep extends UndeployAppStep {

    @Override
    protected StepPhase undeployApplication(CloudControllerClient client, CloudApplication cloudApplicationToUndeploy) {
        if (client.areTasksSupported()) {
            cancelRunningTasks(client, cloudApplicationToUndeploy);
        }

        deleteApplication(client, cloudApplicationToUndeploy);

        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(DelegateExecution context) {
        return MessageFormat.format(Messages.ERROR_DELETING_APP, StepsUtil.getApp(context)
                                                                          .getName());
    }

    private void deleteApplication(CloudControllerClient client, CloudApplication cloudApplicationToUndeploy) {
        getStepLogger().info(Messages.DELETING_APP, cloudApplicationToUndeploy.getName());
        client.deleteApplication(cloudApplicationToUndeploy.getName());
        getStepLogger().debug(Messages.APP_DELETED, cloudApplicationToUndeploy.getName());
    }

    private void cancelRunningTasks(CloudControllerClient client, CloudApplication appToUndeploy) {
        List<CloudTask> tasksToCancel = client.getTasks(appToUndeploy.getName());
        for (CloudTask task : tasksToCancel) {
            CloudTask.State taskState = task.getState();
            if (taskState.equals(CloudTask.State.RUNNING) || taskState.equals(CloudTask.State.PENDING)) {
                cancelTask(client, task, appToUndeploy);
            }
        }
    }

    private void cancelTask(CloudControllerClient client, CloudTask task, CloudApplication appToUndeploy) {
        getStepLogger().info(Messages.CANCELING_TASK_ON_APP, task.getName(), appToUndeploy.getName());
        client.cancelTask(task.getMetadata()
                              .getGuid());
        getStepLogger().debug(Messages.CANCELED_TASK_ON_APP, task.getName(), appToUndeploy.getName());
    }

}
