package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

@Component("restartUpdatedSubscribersStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RestartUpdatedSubscribersStep extends SyncActivitiStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws Exception {
        List<CloudApplication> updatedSubscribers = StepsUtil.getUpdatedSubscribers(execution.getContext());
        for (CloudApplication subscriber : updatedSubscribers) {
            getStepLogger().debug(Messages.UPDATED_SUBSCRIBERS, subscriber.getName());
            restartSubscriber(execution, subscriber);
        }
        return StepPhase.DONE;
    }

    private void restartSubscriber(ExecutionWrapper execution, CloudApplication subscriber) {
        try {
            attemptToRestartApplication(execution, subscriber);
        } catch (CloudOperationException e) {
            getStepLogger().warn(e, Messages.COULD_NOT_RESTART_SUBSCRIBER, subscriber.getName());
        }
    }

    private void attemptToRestartApplication(ExecutionWrapper execution, CloudApplication app) {
        CloudControllerClient client = getClientForApp(execution, app);
        ClientExtensions clientExtensions = getClientExtensionsForApp(execution, app);

        getStepLogger().info(Messages.STOPPING_APP, app.getName());
        client.stopApplication(app.getName());
        getStepLogger().info(Messages.STARTING_APP, app.getName());
        if (clientExtensions != null) {
            clientExtensions.startApplication(app.getName(), false);
        } else {
            client.startApplication(app.getName());
        }
    }

    private CloudControllerClient getClientForApp(ExecutionWrapper execution, CloudApplication app) {
        String orgName = app.getSpace()
            .getOrganization()
            .getName();
        String spaceName = app.getSpace()
            .getName();
        return execution.getCloudControllerClient(orgName, spaceName);
    }

    private ClientExtensions getClientExtensionsForApp(ExecutionWrapper execution, CloudApplication app) {
        String orgName = app.getSpace()
            .getOrganization()
            .getName();
        String spaceName = app.getSpace()
            .getName();
        return execution.getClientExtensions(orgName, spaceName);
    }

}
