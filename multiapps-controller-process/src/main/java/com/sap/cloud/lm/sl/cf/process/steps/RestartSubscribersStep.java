package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.XsCloudControllerClient;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

@Component("restartSubscribersStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RestartSubscribersStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws Exception {
        List<CloudApplication> updatedSubscribers = StepsUtil.getUpdatedSubscribers(execution.getContext());
        for (CloudApplication subscriber : updatedSubscribers) {
            getStepLogger().debug(Messages.UPDATING_SUBSCRIBER_0, subscriber.getName());
            restartSubscriber(execution, subscriber);
        }
        return StepPhase.DONE;
    }

    private void restartSubscriber(ExecutionWrapper execution, CloudApplication subscriber) {
        try {
            attemptToRestartApplication(execution, subscriber);
        } catch (CloudOperationException e) {
            getStepLogger().warn(e, Messages.COULD_NOT_RESTART_SUBSCRIBER_0, subscriber.getName());
        }
    }

    private void attemptToRestartApplication(ExecutionWrapper execution, CloudApplication app) {
        CloudControllerClient client = getClientForApplicationSpace(execution, app);

        getStepLogger().info(Messages.STOPPING_APP, app.getName());
        client.stopApplication(app.getName());
        getStepLogger().info(Messages.STARTING_APP, app.getName());
        if (client instanceof XsCloudControllerClient) {
            ((XsCloudControllerClient) client).startApplication(app.getName(), false);
        } else {
            client.startApplication(app.getName());
        }
    }

    private CloudControllerClient getClientForApplicationSpace(ExecutionWrapper execution, CloudApplication app) {
        CloudSpace space = app.getSpace();
        CloudOrganization organization = space.getOrganization();
        return execution.getControllerClient(organization.getName(), space.getName());
    }

}
