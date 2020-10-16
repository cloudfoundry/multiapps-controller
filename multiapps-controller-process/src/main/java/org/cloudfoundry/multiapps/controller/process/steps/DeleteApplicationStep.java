package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudTask;

@Named("deleteApplicationStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteApplicationStep extends UndeployAppStep {

    @Override
    protected StepPhase undeployApplication(CloudControllerClient client, CloudApplication cloudApplicationToUndeploy,
                                            ProcessContext context) {
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
    protected String getStepErrorMessage(ProcessContext context) {
        return MessageFormat.format(Messages.ERROR_DELETING_APP, context.getVariable(Variables.APP_TO_PROCESS)
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
