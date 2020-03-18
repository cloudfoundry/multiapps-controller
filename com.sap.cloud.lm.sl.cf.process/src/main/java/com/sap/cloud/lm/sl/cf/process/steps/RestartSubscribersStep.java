package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.process.Messages;

@Named("restartSubscribersStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RestartSubscribersStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        List<CloudApplication> updatedSubscribers = StepsUtil.getUpdatedSubscribers(execution.getContext());
        for (CloudApplication subscriber : updatedSubscribers) {
            getStepLogger().debug(Messages.UPDATING_SUBSCRIBER_0, subscriber.getName());
            restartSubscriber(execution, subscriber);
        }
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ExecutionWrapper execution) {
        return Messages.ERROR_RESTARTING_SUBSCRIBERS;
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
        client.startApplication(app.getName());
    }

    private CloudControllerClient getClientForApplicationSpace(ExecutionWrapper execution, CloudApplication app) {
        CloudSpace space = app.getSpace();
        CloudOrganization organization = space.getOrganization();
        return execution.getControllerClient(organization.getName(), space.getName());
    }

}
