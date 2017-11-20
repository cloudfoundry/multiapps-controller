package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

@Component("restartUpdatedSubscribersStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RestartUpdatedSubscribersStep extends SyncActivitiStep {

    @Override
    protected ExecutionStatus executeStep(ExecutionWrapper execution) throws Exception {
        getStepLogger().logActivitiTask();

        List<CloudApplication> updatedSubscribers = StepsUtil.getUpdatedSubscribers(execution.getContext());
        for (CloudApplication subscriber : updatedSubscribers) {
            getStepLogger().debug(Messages.UPDATED_SUBSCRIBERS, subscriber.getName());
            restartSubscriber(execution, subscriber);
        }
        return ExecutionStatus.SUCCESS;
    }

    private void restartSubscriber(ExecutionWrapper execution, CloudApplication subscriber) {
        try {
            attemptToRestartApplication(execution, subscriber);
        } catch (CloudFoundryException e) {
            getStepLogger().warn(e, Messages.COULD_NOT_RESTART_SUBSCRIBER, subscriber.getName());
        }
    }

    private void attemptToRestartApplication(ExecutionWrapper execution, CloudApplication app) {
        CloudFoundryOperations client = getClientForApp(execution, app);
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

    private CloudFoundryOperations getClientForApp(ExecutionWrapper execution, CloudApplication app) {
        String orgName = app.getSpace().getOrganization().getName();
        String spaceName = app.getSpace().getName();
        return execution.getCloudFoundryClient(orgName, spaceName);
    }

    private ClientExtensions getClientExtensionsForApp(ExecutionWrapper execution, CloudApplication app) {
        String orgName = app.getSpace().getOrganization().getName();
        String spaceName = app.getSpace().getName();
        return execution.getClientExtensions(orgName, spaceName);
    }

}
