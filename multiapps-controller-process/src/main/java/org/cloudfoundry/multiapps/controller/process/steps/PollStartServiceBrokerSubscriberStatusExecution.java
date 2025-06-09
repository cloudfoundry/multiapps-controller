package org.cloudfoundry.multiapps.controller.process.steps;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientFactory;
import org.cloudfoundry.multiapps.controller.core.cf.clients.WebClientFactory;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;

public class PollStartServiceBrokerSubscriberStatusExecution extends PollStartAppStatusExecution {

    public PollStartServiceBrokerSubscriberStatusExecution(CloudControllerClientFactory clientFactory,
                                                           TokenService tokenService, ApplicationConfiguration configuration,
                                                           WebClientFactory webClientFactory) {
        super(clientFactory, tokenService, configuration, webClientFactory);
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
