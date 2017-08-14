package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;
import java.util.List;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("restartUpdatedSubscribersStep")
public class RestartUpdatedSubscribersStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestartUpdatedSubscribersStep.class);

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("restartUpdatedSubscribersTask").displayName("Restart Updated Subscribers").description(
            "Restart Updated Subscribers").build();
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws Exception {
        logActivitiTask(context, LOGGER);

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
            warn(context, format(Messages.COULD_NOT_RESTART_SUBSCRIBER, subscriber.getName()), e, LOGGER);
        }
    }

    private void attemptToRestartApplication(DelegateExecution context, CloudApplication app) {
        CloudFoundryOperations client = getClientForApp(context, app);
        ClientExtensions clientExtensions = getClientExtensionsForApp(context, app);

        info(context, format(Messages.STOPPING_APP, app.getName()), LOGGER);
        client.stopApplication(app.getName());
        info(context, format(Messages.STARTING_APP, app.getName()), LOGGER);
        if (clientExtensions != null) {
            clientExtensions.startApplication(app.getName(), false);
        } else {
            client.startApplication(app.getName());
        }
    }

    private CloudFoundryOperations getClientForApp(DelegateExecution context, CloudApplication app) {
        String orgName = app.getSpace().getOrganization().getName();
        String spaceName = app.getSpace().getName();
        return getCloudFoundryClient(context, LOGGER, orgName, spaceName);
    }

    private ClientExtensions getClientExtensionsForApp(DelegateExecution context, CloudApplication app) {
        String orgName = app.getSpace().getOrganization().getName();
        String spaceName = app.getSpace().getName();
        return getClientExtensions(context, LOGGER, orgName, spaceName);
    }

}
