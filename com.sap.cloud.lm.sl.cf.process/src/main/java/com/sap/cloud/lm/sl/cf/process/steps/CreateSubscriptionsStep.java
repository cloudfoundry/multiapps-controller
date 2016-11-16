package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationSubscriptionDao;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription.ResourceDto;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("createSubscriptionsStep")
public class CreateSubscriptionsStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateSubscriptionsStep.class);

    public static StepMetadata getMetadata() {
        return new StepMetadata("createSubscriptionsTask", "Create Subscriptions", "Create Subscriptions");
    }

    @Inject
    private ConfigurationSubscriptionDao dao;

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        logActivitiTask(context, LOGGER);

        try {
            info(context, Messages.CREATING_SUBSCRIPTIONS, LOGGER);

            List<ConfigurationSubscription> subscriptions = StepsUtil.getSubscriptionsToCreate(context);

            for (ConfigurationSubscription subscription : subscriptions) {
                createSubscription(subscription);
            }

            debug(context, Messages.SUBSCRIPTIONS_CREATED, LOGGER);
            return ExecutionStatus.SUCCESS;
        } catch (SLException e) {
            error(context, Messages.ERROR_CREATING_SUBSCRIPTIONS, e, LOGGER);
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
