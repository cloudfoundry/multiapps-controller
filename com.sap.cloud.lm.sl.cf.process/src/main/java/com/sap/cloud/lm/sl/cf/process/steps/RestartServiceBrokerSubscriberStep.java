package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.AsyncStepMetadata;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("restartServiceBrokerSubscriberStep")
public class RestartServiceBrokerSubscriberStep extends RestartAppStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestartServiceBrokerSubscriberStep.class);

    public static StepMetadata getMetadata() {
        return AsyncStepMetadata.builder().id("restartServiceBrokerSubscriberTask").displayName(
            "Restart Service Broker Subscriber").description("Restart Service Broker Subscriber").pollTaskId(
                "pollServiceBrokerRestartStatusTask").build();
    }

    @Override
    protected ExecutionStatus pollStatusInternal(DelegateExecution context) throws SLException {
        try {
            return super.pollStatusInternal(context);
        } catch (CloudFoundryException e) {
            warn(context, format(Messages.FAILED_SERVICE_BROKER_SUBSCRIBER_RESTART, getAppToStart(context).getName()), e, LOGGER);
            return ExecutionStatus.SUCCESS;
        }
    }

    @Override
    protected void onError(DelegateExecution context, String message, Exception e) {
        warn(context, message, e, LOGGER);
    }

    @Override
    protected CloudApplication getAppToStart(DelegateExecution context) {
        return StepsUtil.getServiceBrokerSubscriberToRestart(context);
    }

    @Override
    protected String getIndexVariable() {
        return Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS_INDEX;
    }

}
