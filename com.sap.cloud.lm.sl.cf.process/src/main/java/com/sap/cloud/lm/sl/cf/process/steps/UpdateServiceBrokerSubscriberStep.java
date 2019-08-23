package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;

import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceBroker;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.process.helpers.ExceptionMessageTailMapper;
import com.sap.cloud.lm.sl.cf.process.helpers.ExceptionMessageTailMapper.CloudComponents;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

@Named("updateServiceBrokerSubscriberStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateServiceBrokerSubscriberStep extends CreateOrUpdateServiceBrokerStep {

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
                serviceBroker = ImmutableCloudServiceBroker.copyOf(serviceBroker)
                                                           .withMetadata(existingServiceBroker.getMetadata());
                updateServiceBroker(execution.getContext(), serviceBroker, client);
            }
            return StepPhase.DONE;
        } catch (CloudOperationException coe) {
            CloudControllerException e = new CloudControllerException(coe);
            getStepLogger().warn(MessageFormat.format(Messages.FAILED_SERVICE_BROKER_UPDATE, serviceBroker.getName()), e,
                                 ExceptionMessageTailMapper.map(configuration, CloudComponents.SERVICE_BROKERS, serviceBroker.getName()));
            return StepPhase.DONE;
        }
    }

}
