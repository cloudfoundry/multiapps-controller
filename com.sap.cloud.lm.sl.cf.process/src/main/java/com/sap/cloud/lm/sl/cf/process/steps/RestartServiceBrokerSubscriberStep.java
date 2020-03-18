package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

import javax.inject.Named;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.process.Messages;

@Named("restartServiceBrokerSubscriberStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RestartServiceBrokerSubscriberStep extends RestartAppStep {

    @Override
    protected String getStepErrorMessage(ExecutionWrapper execution) {
        return MessageFormat.format(Messages.ERROR_STARTING_APP_0, getAppToRestart(execution).getName());
    }

    @Override
    protected CloudApplication getAppToRestart(ExecutionWrapper execution) {
        return StepsUtil.getServiceBrokerSubscriberToRestart(execution.getContext());
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ExecutionWrapper execution) {
        return Collections.singletonList(new PollStartServiceBrokerSubscriberStatusExecution(recentLogsRetriever));
    }

}
