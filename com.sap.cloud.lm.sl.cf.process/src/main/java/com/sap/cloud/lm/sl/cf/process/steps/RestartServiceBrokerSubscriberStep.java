package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Arrays;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.process.Constants;

@Component("restartServiceBrokerSubscriberStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RestartServiceBrokerSubscriberStep extends RestartAppStep {

    @Override
    protected void onError(String message, Exception e) {
        getStepLogger().warn(e, message);
    }

    @Override
    protected CloudApplication getAppToRestart(DelegateExecution context) {
        return StepsUtil.getServiceBrokerSubscriberToRestart(context);
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ExecutionWrapper execution) {
        recentLogsRetriever.setFailSafe(true);
        return Arrays.asList(new PollStartServiceBrokerSubscriberStatusExecution(recentLogsRetriever));
    }

    @Override
    protected String getIndexVariable() {
        return Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS_INDEX;
    }

}
