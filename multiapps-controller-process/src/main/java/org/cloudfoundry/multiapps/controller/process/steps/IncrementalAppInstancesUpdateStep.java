package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientFactory;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableIncrementalAppInstanceUpdateConfiguration;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.InstanceInfo;

@Named("incrementalAppInstancesUpdateStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class IncrementalAppInstancesUpdateStep extends TimeoutAsyncFlowableStep {

    @Inject
    protected CloudControllerClientFactory clientFactory;
    @Inject
    protected TokenService tokenService;

    @Override
    public Duration getTimeout(ProcessContext context) {
        return context.getVariable(Variables.START_TIMEOUT);
    }

    @Override
    protected StepPhase executeAsyncStep(ProcessContext context) throws Exception {
        CloudApplicationExtended application = context.getVariable(Variables.APP_TO_PROCESS);
        DeployedMtaApplication oldApplication = getOldApplication(context, application);
        CloudControllerClient client = context.getControllerClient();

        context.getStepLogger()
               .debug("Starting incremental application instance update for \"{0}\"...", application.getName());
        if (oldApplication == null) {
            context.getStepLogger()
                   .debug("Due to missing productive deployed application of module \"{0}\", the new application will be scaled in standard way",
                          application.getModuleName());
            client.updateApplicationInstances(application.getName(), application.getInstances());
            return StepPhase.DONE;
        }

        List<InstanceInfo> oldApplicationInstances = client.getApplicationInstances(oldApplication.getGuid())
                                                           .getInstances();
        ImmutableIncrementalAppInstanceUpdateConfiguration.Builder incrementalAppInstanceUpdateConfigurationBuilder = ImmutableIncrementalAppInstanceUpdateConfiguration.builder();
        if (oldApplicationInstances.size() > 1) {
            int oldApplicationInstancesCount = oldApplicationInstances.size() - 1;
            context.getStepLogger()
                   .info("Downscale application \"{0}\" to {1} instances", oldApplication.getName(), oldApplicationInstancesCount);
            client.updateApplicationInstances(oldApplication.getName(), oldApplicationInstancesCount);
            incrementalAppInstanceUpdateConfigurationBuilder.oldApplication(oldApplication);
            incrementalAppInstanceUpdateConfigurationBuilder.oldApplicationInstanceCount(oldApplicationInstancesCount);
        }

        CloudApplication newApplication = context.getVariable(Variables.EXISTING_APP_TO_POLL);
        int newApplicationInstancesCount = 2;
        context.getStepLogger()
               .info("Upscale application \"{0}\" to {1} instances", newApplication.getName(), newApplicationInstancesCount);
        client.updateApplicationInstances(newApplication.getName(), newApplicationInstancesCount);
        incrementalAppInstanceUpdateConfigurationBuilder.newApplication(newApplication);
        incrementalAppInstanceUpdateConfigurationBuilder.newApplicationInstanceCount(newApplicationInstancesCount);
        context.setVariable(Variables.INCREMENTAL_APP_INSTANCE_UPDATE_CONFIGURATION,
                            incrementalAppInstanceUpdateConfigurationBuilder.build());

        return StepPhase.POLL;
    }

    private DeployedMtaApplication getOldApplication(ProcessContext context, CloudApplicationExtended currentApplication) {
        DeployedMta deployedMta = context.getVariable(Variables.DEPLOYED_MTA);
        if (deployedMta == null) {
            return null;
        }
        return deployedMta.getApplications()
                          .stream()
                          .filter(deployedApplication -> deployedApplication.getModuleName()
                                                                            .equals(currentApplication.getModuleName()))
                          .findFirst()
                          .orElse(null);

    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ProcessContext context) {
        return List.of(new PollStartAppStatusExecution(clientFactory, tokenService), new PollIncrementalAppInstanceUpdateExecution());
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return MessageFormat.format("Error during incremental instance update of module \"{0}\"",
                                    context.getVariable(Variables.APP_TO_PROCESS)
                                           .getModuleName());
    }

}
