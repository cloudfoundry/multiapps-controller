package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.core.model.BlueGreenApplicationNameSuffix;
import org.cloudfoundry.multiapps.controller.core.model.ConfigurationSubscription;
import org.cloudfoundry.multiapps.controller.core.persistence.service.ConfigurationSubscriptionService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("removeNewApplicationsSuffixStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RemoveNewApplicationsSuffixStep extends SyncFlowableStep {

    @Inject
    private ConfigurationSubscriptionService subscriptionService;

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        if (!context.getVariable(Variables.KEEP_ORIGINAL_APP_NAMES_AFTER_DEPLOY)) {
            return StepPhase.DONE;
        }

        List<String> appsToProcess = context.getVariable(Variables.APPS_TO_DEPLOY);
        CloudControllerClient client = context.getControllerClient();

        for (String appName : appsToProcess) {
            String newName = BlueGreenApplicationNameSuffix.removeSuffix(appName);
            getStepLogger().info(Messages.RENAMING_APPLICATION_0_TO_1, appName, newName);
            client.rename(appName, newName);
        }

        String mtaId = context.getVariable(Variables.MTA_ID);
        String spaceId = context.getVariable(Variables.SPACE_GUID);
        updateConfigurationSubscribers(appsToProcess, mtaId, spaceId);

        return StepPhase.DONE;
    }

    private void updateConfigurationSubscribers(List<String> appsToProcess, String mtaId, String spaceId) {
        List<ConfigurationSubscription> subscriptions = subscriptionService.createQuery()
                                                                           .mtaId(mtaId)
                                                                           .spaceId(spaceId)
                                                                           .list();
        for (ConfigurationSubscription subscription : subscriptions) {
            if (appsToProcess.contains(subscription.getAppName())) {
                String newAppName = BlueGreenApplicationNameSuffix.removeSuffix(subscription.getAppName());
                getStepLogger().debug(Messages.UPDATING_CONFIGURATION_SUBSCRIPTION_0_WITH_NAME_1, subscription.getAppName(), newAppName);
                updateConfigurationSubscription(subscription, newAppName);
            }
        }
    }

    private void updateConfigurationSubscription(ConfigurationSubscription subscription, String newAppName) {
        ConfigurationSubscription newSubscription = createNewSubscription(subscription, newAppName);
        subscriptionService.update(subscription, newSubscription);
    }

    private ConfigurationSubscription createNewSubscription(ConfigurationSubscription subscription, String newAppName) {
        return new ConfigurationSubscription(subscription.getId(),
                                             subscription.getMtaId(),
                                             subscription.getSpaceId(),
                                             newAppName,
                                             subscription.getFilter(),
                                             subscription.getModuleDto(),
                                             subscription.getResourceDto(),
                                             subscription.getModuleId(),
                                             subscription.getResourceId());
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_RENAMING_NEW_APPLICATIONS;
    }

}
