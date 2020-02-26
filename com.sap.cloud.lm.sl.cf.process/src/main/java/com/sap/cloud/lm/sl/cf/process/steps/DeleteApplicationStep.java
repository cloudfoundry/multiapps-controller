package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.List;

import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudTask;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.process.Messages;

@Named("deleteApplicationStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteApplicationStep extends UndeployAppStep {

    @Override
    protected StepPhase undeployApplication(CloudControllerClient client, CloudApplication cloudApplicationToUndeploy) {
        String applicationName = cloudApplicationToUndeploy.getName();
        try {
            cancelRunningTasks(client, applicationName);
            deleteApplication(client, applicationName);
        } catch (CloudOperationException e) {
            ignoreApplicationNotFound(e, cloudApplicationToUndeploy.getName());
        }
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(DelegateExecution context) {
        return MessageFormat.format(Messages.ERROR_DELETING_APP, StepsUtil.getApp(context)
                                                                          .getName());
    }

    private void deleteApplication(CloudControllerClient client, String applicationName) {
        getStepLogger().info(Messages.DELETING_APP, applicationName);
        client.deleteApplication(applicationName);
        getStepLogger().debug(Messages.APP_DELETED, applicationName);
    }

    private void ignoreApplicationNotFound(CloudOperationException e, String applicationName) {
        if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
            getStepLogger().info(Messages.APP_NOT_FOUND, applicationName);
            return;
        }
        throw e;
    }

    private void cancelRunningTasks(CloudControllerClient client, String applicationName) {
        List<CloudTask> tasksToCancel = client.getTasks(applicationName);
        for (CloudTask task : tasksToCancel) {
            CloudTask.State taskState = task.getState();
            if (taskState.equals(CloudTask.State.RUNNING) || taskState.equals(CloudTask.State.PENDING)) {
                cancelTask(client, task, applicationName);
            }
        }
    }

    private void cancelTask(CloudControllerClient client, CloudTask task, String applicationName) {
        getStepLogger().info(Messages.CANCELING_TASK_ON_APP, task.getName(), applicationName);
        client.cancelTask(task.getMetadata()
                              .getGuid());
        getStepLogger().debug(Messages.CANCELED_TASK_ON_APP, task.getName(), applicationName);
    }

}
