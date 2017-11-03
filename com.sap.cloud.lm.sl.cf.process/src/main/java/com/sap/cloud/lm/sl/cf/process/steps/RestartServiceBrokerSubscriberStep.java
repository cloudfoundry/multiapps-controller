package com.sap.cloud.lm.sl.cf.process.steps;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Component("restartServiceBrokerSubscriberStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RestartServiceBrokerSubscriberStep extends RestartAppStep {

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        try {
            return super.executeStepInternal(context);
        } catch (CloudFoundryException e) {
            getStepLogger().warn(e, Messages.FAILED_SERVICE_BROKER_SUBSCRIBER_RESTART, getAppToStart(context).getName());
            return ExecutionStatus.SUCCESS;
        }
    }

    @Override
    protected void onError(String message, Exception e) {
        getStepLogger().warn(e, message);
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
