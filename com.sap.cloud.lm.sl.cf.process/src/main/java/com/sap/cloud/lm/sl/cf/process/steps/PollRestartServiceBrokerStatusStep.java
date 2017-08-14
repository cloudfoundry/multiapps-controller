package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Component("pollServiceBrokerRestartStatus")
public class PollRestartServiceBrokerStatusStep extends PollStartAppStatusStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollRestartServiceBrokerStatusStep.class);

    @Override
    protected ExecutionStatus pollStatusInternal(DelegateExecution context) throws SLException {
        try {
            ExecutionStatus status = super.pollStatusInternal(context);
            if (status.equals(ExecutionStatus.LOGICAL_RETRY)) {
                status = ExecutionStatus.SUCCESS;
            }
            return status;
        } catch (SLException e) {
            warn(context, format(Messages.FAILED_SERVICE_BROKER_START, getAppToPoll(context).getName()), e, LOGGER);
            return ExecutionStatus.SUCCESS;
        }
    }

    @Override
    protected CloudApplicationExtended getAppToPoll(DelegateExecution context) {
        return StepsUtil.getServiceBrokerSubscriberToRestart(context);
    }

    @Override
    public String getLogicalStepName() {
        return RestartServiceBrokerSubscriberStep.class.getSimpleName();
    }

    @Override
    protected String getIndexVariable() {
        return Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS_INDEX;
    }

    @Override
    protected void onError(DelegateExecution context, String message, Exception e) {
        warn(context, message, e, LOGGER);
    }

    @Override
    protected void onError(DelegateExecution context, String message) {
        warn(context, message, LOGGER);
    }

}
