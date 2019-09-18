package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription.ResourceDto;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationSubscriptionService;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Named("deleteSubscriptionsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteSubscriptionsStep extends SyncFlowableStep {

    @Inject
    private ConfigurationSubscriptionService configurationSubscriptionService;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        getStepLogger().debug(Messages.DELETING_SUBSCRIPTIONS);

        List<ConfigurationSubscription> subscriptionsToDelete = StepsUtil.getSubscriptionsToDelete(execution.getContext());
        getStepLogger().debug(Messages.SUBSCRIPTIONS_TO_DELETE, JsonUtil.toJson(subscriptionsToDelete, true));
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
    protected String getStepErrorMessage(DelegateExecution context) {
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
