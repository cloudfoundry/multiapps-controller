package org.cloudfoundry.multiapps.controller.process.steps;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientFactory;
import org.cloudfoundry.multiapps.controller.core.cf.clients.WebClientFactory;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

public class PollStartLiveAppExecution extends PollStartAppStatusExecution {

    public PollStartLiveAppExecution(CloudControllerClientFactory clientFactory, TokenService tokenService,
                                     ApplicationConfiguration configuration, WebClientFactory webClientFactory) {
        super(clientFactory, tokenService, configuration, webClientFactory);
    }

    @Override
    protected CloudApplication getAppToPoll(ProcessContext context) {
        return getDeployedMtaApplication(context);
    }

    private DeployedMtaApplication getDeployedMtaApplication(ProcessContext context) {
        CloudApplicationExtended application = context.getVariable(Variables.APP_TO_PROCESS);
        return context.getVariable(Variables.DEPLOYED_MTA)
                      .getApplications()
                      .stream()
                      .filter(deployedApplication -> deployedApplication.getModuleName()
                                                                        .equals(application.getModuleName()))
                      .findFirst()
                      .orElseThrow(() -> new SLException(Messages.REQUIRED_APPLICATION_TO_POLL_0_NOT_FOUND, application.getName()));
    }

    @Override
    protected CloudApplication getApplication(ProcessContext context, String appToPoll, CloudControllerClient client) {
        return getDeployedMtaApplication(context);
    }
}
