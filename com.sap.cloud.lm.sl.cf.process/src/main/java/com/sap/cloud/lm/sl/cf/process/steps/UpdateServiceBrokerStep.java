package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationAttributesGetter;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("updateServiceBrokerStep")
public class UpdateServiceBrokerStep extends CreateServiceBrokersStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateServiceBrokerStep.class);

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("updateServiceBrokerTask").displayName("Update Service Broker Step").description(
            "Update Service Broker Step").build();
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        logActivitiTask(context, LOGGER);

        CloudApplication serviceBrokerAppication = StepsUtil.getServiceBrokerSubscriberToRestart(context);
        ApplicationAttributesGetter attributesGetter = ApplicationAttributesGetter.forApplication(serviceBrokerAppication);
        String serviceBrokerName = attributesGetter.getAttribute(SupportedParameters.SERVICE_BROKER_NAME, String.class);

        try {
            CloudFoundryOperations client = getCloudFoundryClient(context, LOGGER);
            CloudServiceBroker serviceBroker = client.getServiceBroker(serviceBrokerName);
            updateServiceBroker(context, serviceBroker, client);
            return ExecutionStatus.SUCCESS;
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            warn(context, format(Messages.FAILED_SERVICE_BROKER_UPDATE, serviceBrokerName), e, LOGGER);
            return ExecutionStatus.SUCCESS;
        }
    }
}
