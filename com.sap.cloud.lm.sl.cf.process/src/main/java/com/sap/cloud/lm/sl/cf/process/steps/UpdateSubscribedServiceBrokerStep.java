package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;

import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceBrokerExtended;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Component("updateSubscribedServiceBrokerStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateSubscribedServiceBrokerStep extends CreateOrUpdateServiceBrokersStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws SLException {
        CloudApplication serviceBrokerAppication = StepsUtil.getServiceBrokerSubscriberToRestart(execution.getContext());
        CloudServiceBrokerExtended broker = getServiceBrokerFromApp(serviceBrokerAppication, execution.getContext());

        try {
            CloudControllerClient client = execution.getCloudControllerClient();
            CloudServiceBroker existingServiceBroker = client.getServiceBroker(broker.getName(), false);
            if (existingServiceBroker == null) {
                getStepLogger().warn(MessageFormat.format(Messages.SERVICE_BROKER_DOES_NOT_EXIST, broker.getName()));
            } else {
                broker.setMeta(existingServiceBroker.getMeta());
                updateServiceBroker(execution.getContext(), broker, client);
            }
            return StepPhase.DONE;
        } catch (CloudOperationException coe) {
            CloudControllerException e = new CloudControllerException(coe);
            getStepLogger().warn(e, Messages.FAILED_SERVICE_BROKER_UPDATE, broker.getName());
            return StepPhase.DONE;
        }
    }

}
