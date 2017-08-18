package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationSubscriptionDao;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription.ResourceDto;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("createSubscriptionsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CreateSubscriptionsStep extends AbstractXS2ProcessStep {

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("createSubscriptionsTask").displayName("Create Subscriptions").description(
            "Create Subscriptions").build();
    }

    @Inject
    private ConfigurationSubscriptionDao dao;

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        getStepLogger().logActivitiTask();

        try {
            getStepLogger().info(Messages.CREATING_SUBSCRIPTIONS);

            List<ConfigurationSubscription> subscriptions = StepsUtil.getSubscriptionsToCreate(context);

            for (ConfigurationSubscription subscription : subscriptions) {
                createSubscription(subscription);
            }

            getStepLogger().debug(Messages.SUBSCRIPTIONS_CREATED);
            return ExecutionStatus.SUCCESS;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_CREATING_SUBSCRIPTIONS);
            throw e;
        }
    }

    private ConfigurationSubscription detectSubscription(String mtaId, String applicationName, String spaceId, String resourceName) {
        List<ConfigurationSubscription> subscriptions = dao.findAll(mtaId, applicationName, spaceId, resourceName);
        if ((subscriptions.size() != 0)) {
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
