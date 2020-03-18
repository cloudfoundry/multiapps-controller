package com.sap.cloud.lm.sl.cf.process.steps;

import org.cloudfoundry.client.lib.domain.CloudApplication;

import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;

public class PollStartServiceBrokerSubscriberStatusExecution extends PollStartAppStatusExecution {

    public PollStartServiceBrokerSubscriberStatusExecution(RecentLogsRetriever recentLogsRetriever) {
        super(recentLogsRetriever);
    }

    @Override
    protected void onError(ExecutionWrapper execution, String message, Object... arguments) {
        execution.getStepLogger()
                 .warn(message, arguments);
    }

    @Override
    protected CloudApplication getAppToPoll(ExecutionWrapper execution) {
        return StepsUtil.getUpdatedServiceBrokerSubscriber(execution);
    }

}
