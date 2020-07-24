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
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

@Named("restartSubscribersStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RestartSubscribersStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        List<CloudApplication> updatedSubscribers = context.getVariable(Variables.UPDATED_SUBSCRIBERS);
        for (CloudApplication subscriber : updatedSubscribers) {
            getStepLogger().debug(Messages.UPDATING_SUBSCRIBER_0, subscriber.getName());
            restartSubscriber(context, subscriber);
        }
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_RESTARTING_SUBSCRIBERS;
    }

    private void restartSubscriber(ProcessContext context, CloudApplication subscriber) {
        try {
            attemptToRestartApplication(context, subscriber);
        } catch (CloudOperationException e) {
            getStepLogger().warn(e, Messages.COULD_NOT_RESTART_SUBSCRIBER_0, subscriber.getName());
        }
    }

    private void attemptToRestartApplication(ProcessContext context, CloudApplication app) {
        CloudControllerClient client = getClientForApplicationSpace(context, app);

        getStepLogger().info(Messages.STOPPING_APP, app.getName());
        client.stopApplication(app.getName());
        getStepLogger().info(Messages.STARTING_APP, app.getName());
        client.startApplication(app.getName());
    }

    private CloudControllerClient getClientForApplicationSpace(ProcessContext context, CloudApplication app) {
        CloudSpace space = app.getSpace();
        CloudOrganization organization = space.getOrganization();
        return context.getControllerClient(organization.getName(), space.getName());
    }

}
