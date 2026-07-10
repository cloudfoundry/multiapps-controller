package org.cloudfoundry.multiapps.controller.process.steps;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientFactory;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.core.cloudlogging.OperationLogsExporter;

public class PollStartServiceBrokerSubscriberStatusExecution extends PollStartAppStatusExecution {

    public PollStartServiceBrokerSubscriberStatusExecution(CloudControllerClientFactory clientFactory, TokenService tokenService,
                                                           OperationLogsExporter operationLogsExporter) {
        super(clientFactory, tokenService, operationLogsExporter);
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
