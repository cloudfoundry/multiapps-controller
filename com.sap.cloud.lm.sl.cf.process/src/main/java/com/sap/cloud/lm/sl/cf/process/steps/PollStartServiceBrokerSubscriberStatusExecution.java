package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

public class PollStartServiceBrokerSubscriberStatusExecution extends PollStartAppStatusExecution {

    public PollStartServiceBrokerSubscriberStatusExecution(RecentLogsRetriever recentLogsRetriever) {
        super(recentLogsRetriever);
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
