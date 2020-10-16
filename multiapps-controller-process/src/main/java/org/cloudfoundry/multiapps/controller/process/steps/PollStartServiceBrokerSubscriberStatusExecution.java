package org.cloudfoundry.multiapps.controller.process.steps;

import org.cloudfoundry.multiapps.controller.core.cf.clients.RecentLogsRetriever;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;

public class PollStartServiceBrokerSubscriberStatusExecution extends PollStartAppStatusExecution {

    public PollStartServiceBrokerSubscriberStatusExecution(RecentLogsRetriever recentLogsRetriever) {
        super(recentLogsRetriever);
    }

    @Override
    protected void onError(ProcessContext context, String message, Object... arguments) {
        context.getStepLogger()
               .warn(message, arguments);
    }

    @Override
    protected CloudApplication getAppToPoll(ProcessContext context) {
        return StepsUtil.getUpdatedServiceBrokerSubscriber(context);
    }

}
