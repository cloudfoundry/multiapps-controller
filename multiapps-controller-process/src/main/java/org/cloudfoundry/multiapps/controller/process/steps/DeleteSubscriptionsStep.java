package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerialization;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationSubscription;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationSubscription.ResourceDto;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationSubscriptionService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("deleteSubscriptionsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteSubscriptionsStep extends SyncFlowableStep {

    @Inject
    private ConfigurationSubscriptionService configurationSubscriptionService;

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.DELETING_SUBSCRIPTIONS);

        List<ConfigurationSubscription> subscriptionsToDelete = context.getVariable(Variables.SUBSCRIPTIONS_TO_DELETE);
        getStepLogger().debug(Messages.SUBSCRIPTIONS_TO_DELETE, SecureSerialization.toJson(subscriptionsToDelete));
        for (ConfigurationSubscription subscription : subscriptionsToDelete) {
            infoSubscriptionDeletion(subscription);
            int removedSubscriptions = configurationSubscriptionService.createQuery()
                                                                       .id(subscription.getId())
                                                                       .delete();
            if (removedSubscriptions == 0) {
                ResourceDto resourceDto = subscription.getResourceDto();
                getStepLogger().warn(Messages.COULD_NOT_DELETE_SUBSCRIPTION, subscription.getAppName(), getName(resourceDto));
            }
        }

        getStepLogger().debug(Messages.DELETED_SUBSCRIPTIONS);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_DELETING_SUBSCRIPTIONS;
    }

    private void infoSubscriptionDeletion(ConfigurationSubscription subscription) {
        if (subscription.getModuleDto() != null && subscription.getResourceDto() != null) {
            getStepLogger().info(MessageFormat.format(Messages.DELETING_DISCONTINUED_SUBSCRIPTION_FROM_0_MODULE_TO_1_RESOURCE,
                                                      subscription.getModuleDto()
                                                                  .getName(),
                                                      subscription.getResourceDto()
                                                                  .getName()));
        }
    }

    private String getName(ResourceDto dto) {
        return dto == null ? null : dto.getName();
    }

}
