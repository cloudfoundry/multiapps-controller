package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("restartUpdatedSubscribersStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RestartUpdatedSubscribersStep extends AbstractXS2ProcessStep {

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("restartUpdatedSubscribersTask").displayName("Restart Updated Subscribers").description(
            "Restart Updated Subscribers").build();
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws Exception {
        getStepLogger().logActivitiTask();

        List<CloudApplication> updatedSubscribers = StepsUtil.getUpdatedSubscribers(context);
        for (CloudApplication subscriber : updatedSubscribers) {
            restartSubscriber(context, subscriber);
        }
        return ExecutionStatus.SUCCESS;
    }

    private void restartSubscriber(DelegateExecution context, CloudApplication subscriber) {
        try {
            attemptToRestartApplication(context, subscriber);
        } catch (CloudFoundryException e) {
            getStepLogger().warn(e, Messages.COULD_NOT_RESTART_SUBSCRIBER, subscriber.getName());
        }
    }

    private void attemptToRestartApplication(DelegateExecution context, CloudApplication app) {
        CloudFoundryOperations client = getClientForApp(context, app);
        ClientExtensions clientExtensions = getClientExtensionsForApp(context, app);

        getStepLogger().info(Messages.STOPPING_APP, app.getName());
        client.stopApplication(app.getName());
        getStepLogger().info(Messages.STARTING_APP, app.getName());
        if (clientExtensions != null) {
            clientExtensions.startApplication(app.getName(), false);
        } else {
            client.startApplication(app.getName());
        }
    }

    private CloudFoundryOperations getClientForApp(DelegateExecution context, CloudApplication app) {
        String orgName = app.getSpace().getOrganization().getName();
        String spaceName = app.getSpace().getName();
        return getCloudFoundryClient(context, orgName, spaceName);
    }

    private ClientExtensions getClientExtensionsForApp(DelegateExecution context, CloudApplication app) {
        String orgName = app.getSpace().getOrganization().getName();
        String spaceName = app.getSpace().getName();
        return getClientExtensions(context, orgName, spaceName);
    }

}
