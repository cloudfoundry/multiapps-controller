package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import javax.inject.Inject;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationSubscriptionDao;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription.ResourceDto;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Component("createSubscriptionsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CreateSubscriptionsStep extends SyncActivitiStep {

    @Inject
    private ConfigurationSubscriptionDao dao;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws SLException {
        try {
            getStepLogger().info(Messages.CREATING_SUBSCRIPTIONS);

            List<ConfigurationSubscription> subscriptions = StepsUtil.getSubscriptionsToCreate(execution.getContext());

            for (ConfigurationSubscription subscription : subscriptions) {
                createSubscription(subscription);
                getStepLogger().debug(Messages.CREATED_SUBSCRIPTION, subscription.getId());
            }

            getStepLogger().debug(Messages.SUBSCRIPTIONS_CREATED);
            return StepPhase.DONE;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_CREATING_SUBSCRIPTIONS);
            throw e;
        }
    }

    private ConfigurationSubscription detectSubscription(String mtaId, String applicationName, String spaceId, String resourceName) {
        List<ConfigurationSubscription> subscriptions = dao.findAll(mtaId, applicationName, spaceId, resourceName);
        if (!subscriptions.isEmpty()) {
            return subscriptions.get(0); // There's a unique constraint on these parameters, so
                                         // there should be only one such configuration
                                         // subscription.
        }
        return null;
    }

    private ConfigurationSubscription detectSubscription(ConfigurationSubscription subscription) {
        ResourceDto resourceDto = subscription.getResourceDto();
        if (resourceDto == null) {
            return null;
        }
        return detectSubscription(subscription.getMtaId(), subscription.getAppName(), subscription.getSpaceId(), resourceDto.getName());
    }

    protected void createSubscription(ConfigurationSubscription subscription) throws SLException {
        ConfigurationSubscription existingSubscription = detectSubscription(subscription);
        if (existingSubscription != null) {
            dao.update(existingSubscription.getId(), subscription);
        } else {
            dao.add(subscription);
        }
    }

}
