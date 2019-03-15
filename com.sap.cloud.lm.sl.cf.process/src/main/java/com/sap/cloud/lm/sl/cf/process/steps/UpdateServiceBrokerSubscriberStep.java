package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.process.message.Messages;

@Component("updateServiceBrokerSubscriberStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateServiceBrokerSubscriberStep extends CreateOrUpdateServiceBrokersStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        CloudApplication serviceBrokerAppication = StepsUtil.getServiceBrokerSubscriberToRestart(execution.getContext());
        CloudServiceBroker serviceBroker = getServiceBrokerFromApp(serviceBrokerAppication, execution.getContext());

        try {
            CloudControllerClient client = execution.getControllerClient();
            CloudServiceBroker existingServiceBroker = client.getServiceBroker(serviceBroker.getName(), false);
            if (existingServiceBroker == null) {
                getStepLogger().warn(MessageFormat.format(Messages.SERVICE_BROKER_DOES_NOT_EXIST, serviceBroker.getName()));
            } else {
                serviceBroker.setMeta(existingServiceBroker.getMeta());
                updateServiceBroker(execution.getContext(), serviceBroker, client);
            }
            return StepPhase.DONE;
        } catch (CloudOperationException coe) {
            CloudControllerException e = new CloudControllerException(coe);
            getStepLogger().warn(e, Messages.FAILED_SERVICE_BROKER_UPDATE, serviceBroker.getName());
            return StepPhase.DONE;
        }
    }

}
