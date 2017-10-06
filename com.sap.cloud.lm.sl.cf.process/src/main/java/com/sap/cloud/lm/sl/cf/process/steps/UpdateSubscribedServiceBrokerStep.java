package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceBrokerExtended;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("updateSubscribedServiceBrokerStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateSubscribedServiceBrokerStep extends CreateOrUpdateServiceBrokersStep {

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("updateSubscribedServiceBrokerTask").displayName(
            "Update Subscribed Service Broker Step").description("Update Subscribed Service Broker Step").build();
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        getStepLogger().logActivitiTask();

        CloudApplication serviceBrokerAppication = StepsUtil.getServiceBrokerSubscriberToRestart(context);
        CloudServiceBrokerExtended broker = getServiceBrokerFromApp(serviceBrokerAppication, context);

        try {
            CloudFoundryOperations client = getCloudFoundryClient(context);
            CloudServiceBroker serviceBroker = client.getServiceBroker(broker.getName());
            if (serviceBroker == null) {
                getStepLogger().warn(MessageFormat.format(Messages.SERVICE_BROKER_DOES_NOT_EXIST, broker.getName()));
            } else {
                updateServiceBroker(context, broker, client);
            }
            return ExecutionStatus.SUCCESS;
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            getStepLogger().warn(e, Messages.FAILED_SERVICE_BROKER_UPDATE, broker.getName());
            return ExecutionStatus.SUCCESS;
        }
    }

}
