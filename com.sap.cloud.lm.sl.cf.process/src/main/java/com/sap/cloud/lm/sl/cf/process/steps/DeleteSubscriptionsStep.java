package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

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
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("deleteSubscriptionsStep")
public class DeleteSubscriptionsStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteSubscriptionsStep.class);

    public static StepMetadata getMetadata() {
        return new StepMetadata("deleteSubscriptionsTask", "Delete Subscriptions", "Delete Subscriptions");
    }

    @Inject
    private ConfigurationSubscriptionDao dao;

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) {
        logActivitiTask(context, LOGGER);

        info(context, Messages.DELETING_SUBSCRIPTIONS, LOGGER);

        List<ConfigurationSubscription> subscriptionsToDelete = StepsUtil.getSubscriptionsToDelete(context);
        for (ConfigurationSubscription subscription : subscriptionsToDelete) {
            try {
                dao.remove(subscription.getId());
            } catch (NotFoundException e) {
                ResourceDto resourceDto = subscription.getResourceDto();
                warn(context, format(Messages.COULD_NOT_DELETE_SUBSCRIPTION, subscription.getAppName(), getName(resourceDto)), LOGGER);
            }
        }

        debug(context, Messages.DELETED_SUBSCRIPTIONS, LOGGER);
        return ExecutionStatus.SUCCESS;
    }

    private String getName(ResourceDto dto) {
        return dto == null ? null : dto.getName();
    }

}
