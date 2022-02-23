package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ExceptionMessageTailMapper;
import org.cloudfoundry.multiapps.controller.process.util.ExceptionMessageTailMapper.CloudComponents;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudControllerException;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBroker;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceBroker;

@Named("updateServiceBrokerSubscriberStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateServiceBrokerSubscriberStep extends CreateOrUpdateServiceBrokerStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        CloudApplication serviceBrokerApplication = StepsUtil.getUpdatedServiceBrokerSubscriber(context);
        CloudServiceBroker serviceBroker = getServiceBrokerFromApp(context, serviceBrokerApplication);

        try {
            CloudControllerClient client = context.getControllerClient();
            CloudServiceBroker existingServiceBroker = client.getServiceBroker(serviceBroker.getName(), false);
            if (existingServiceBroker == null) {
                getStepLogger().warn(MessageFormat.format(Messages.SERVICE_BROKER_DOES_NOT_EXIST, serviceBroker.getName()));
            } else {
                serviceBroker = ImmutableCloudServiceBroker.copyOf(serviceBroker)
                                                           .withMetadata(existingServiceBroker.getMetadata());
                updateServiceBroker(context, serviceBroker, client);
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
