package com.sap.cloud.lm.sl.cf.process.steps;

import org.activiti.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Component("pollServiceBrokerRestartStatus")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PollRestartServiceBrokerStatusStep extends PollStartAppStatusStep {

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        try {
            ExecutionStatus status = super.executeStepInternal(context);
            if (status.equals(ExecutionStatus.LOGICAL_RETRY)) {
                status = ExecutionStatus.SUCCESS;
            }
            return status;
        } catch (SLException e) {
            getStepLogger().warn(e, Messages.FAILED_SERVICE_BROKER_START, getAppToPoll(context).getName());
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
    protected void onError(String message, Exception e) {
        getStepLogger().warn(e, message);
    }

    @Override
    protected void onError(String message) {
        getStepLogger().warn(message);
    }

}
