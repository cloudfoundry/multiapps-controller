package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudSpace;

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
        return context.getControllerClient(space.getGuid()
                                                .toString());
    }

}
