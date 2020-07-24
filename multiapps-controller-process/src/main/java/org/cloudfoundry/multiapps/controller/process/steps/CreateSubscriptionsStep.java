package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.NoResultException;

import org.cloudfoundry.multiapps.controller.core.model.ConfigurationSubscription;
import org.cloudfoundry.multiapps.controller.core.model.ConfigurationSubscription.ResourceDto;
import org.cloudfoundry.multiapps.controller.core.persistence.service.ConfigurationSubscriptionService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("createSubscriptionsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CreateSubscriptionsStep extends SyncFlowableStep {

    @Inject
    private ConfigurationSubscriptionService configurationSubscriptionService;

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.CREATING_SUBSCRIPTIONS);

        List<ConfigurationSubscription> subscriptions = context.getVariable(Variables.SUBSCRIPTIONS_TO_CREATE);

        for (ConfigurationSubscription subscription : subscriptions) {
            createSubscription(subscription);
            getStepLogger().debug(Messages.CREATED_SUBSCRIPTION, subscription.getId());
        }

        getStepLogger().debug(Messages.SUBSCRIPTIONS_CREATED);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_CREATING_SUBSCRIPTIONS;
    }

    protected void createSubscription(ConfigurationSubscription subscription) {
        infoSubscriptionCreation(subscription);
        ConfigurationSubscription existingSubscription = detectSubscription(subscription);
        if (existingSubscription != null) {
            configurationSubscriptionService.update(existingSubscription, subscription);
            return;
        }
        configurationSubscriptionService.add(subscription);
    }

    private void infoSubscriptionCreation(ConfigurationSubscription subscription) {
        if (subscription.getModuleDto() != null && subscription.getResourceDto() != null) {
            getStepLogger().info(MessageFormat.format(Messages.CREATING_SUBSCRIPTION_FROM_0_MODULE_TO_1_RESOURCE,
                                                      subscription.getModuleDto()
                                                                  .getName(),
                                                      subscription.getResourceDto()
                                                                  .getName()));
        }
    }

    private ConfigurationSubscription detectSubscription(ConfigurationSubscription subscription) {
        ResourceDto resourceDto = subscription.getResourceDto();
        if (resourceDto == null) {
            return null;
        }
        return detectSubscription(subscription.getMtaId(), subscription.getAppName(), subscription.getSpaceId(), resourceDto.getName());
    }

    private ConfigurationSubscription detectSubscription(String mtaId, String applicationName, String spaceId, String resourceName) {
        try {
            return configurationSubscriptionService.createQuery()
                                                   .appName(applicationName)
                                                   .spaceId(spaceId)
                                                   .resourceName(resourceName)
                                                   .mtaId(mtaId)
                                                   .singleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

}
