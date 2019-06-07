package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Arrays;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.process.message.Messages;

@Component("restartServiceBrokerSubscriberStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RestartServiceBrokerSubscriberStep extends RestartAppStep {

    @Override
    protected void onStepError(DelegateExecution context, Exception e) throws Exception {
        getStepLogger().warn(e, Messages.ERROR_STARTING_APP_1, getAppToRestart(context).getName());
        throw e;
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

}
