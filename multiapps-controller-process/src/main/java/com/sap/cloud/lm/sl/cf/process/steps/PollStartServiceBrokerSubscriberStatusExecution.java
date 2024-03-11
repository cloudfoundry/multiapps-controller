package com.sap.cloud.lm.sl.cf.process.steps;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;

public class PollStartServiceBrokerSubscriberStatusExecution extends PollStartAppStatusExecution {

    public PollStartServiceBrokerSubscriberStatusExecution(RecentLogsRetriever recentLogsRetriever,
                                                           ApplicationConfiguration configuration) {
        super(recentLogsRetriever, configuration);
    }

    @Override
    protected void onError(ExecutionWrapper execution, String message, Exception e) {
        execution.getStepLogger()
                 .warn(e, message);
    }

    @Override
    protected void onError(ExecutionWrapper execution, String message) {
        execution.getStepLogger()
                 .warn(message);
    }

    @Override
    protected CloudApplication getAppToPoll(DelegateExecution context) {
        return StepsUtil.getServiceBrokerSubscriberToRestart(context);
    }

}
