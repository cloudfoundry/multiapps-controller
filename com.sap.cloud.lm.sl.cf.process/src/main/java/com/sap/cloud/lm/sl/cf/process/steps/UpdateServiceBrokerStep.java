package com.sap.cloud.lm.sl.cf.process.steps;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationAttributesGetter;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("updateServiceBrokerStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateServiceBrokerStep extends CreateServiceBrokersStep {

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("updateServiceBrokerTask").displayName("Update Service Broker Step").description(
            "Update Service Broker Step").build();
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        getStepLogger().logActivitiTask();

        CloudApplication serviceBrokerAppication = StepsUtil.getServiceBrokerSubscriberToRestart(context);
        ApplicationAttributesGetter attributesGetter = ApplicationAttributesGetter.forApplication(serviceBrokerAppication);
        String serviceBrokerName = attributesGetter.getAttribute(SupportedParameters.SERVICE_BROKER_NAME, String.class);

        try {
            CloudFoundryOperations client = getCloudFoundryClient(context);
            CloudServiceBroker serviceBroker = client.getServiceBroker(serviceBrokerName);
            updateServiceBroker(context, serviceBroker, client);
            return ExecutionStatus.SUCCESS;
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            getStepLogger().warn(e, Messages.FAILED_SERVICE_BROKER_UPDATE, serviceBrokerName);
            return ExecutionStatus.SUCCESS;
        }
    }

}
