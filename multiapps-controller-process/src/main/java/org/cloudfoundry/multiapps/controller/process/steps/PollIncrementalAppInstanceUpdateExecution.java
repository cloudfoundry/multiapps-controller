package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableIncrementalAppInstanceUpdateConfiguration;
import org.cloudfoundry.multiapps.controller.core.model.IncrementalAppInstanceUpdateConfiguration;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;

public class PollIncrementalAppInstanceUpdateExecution implements AsyncExecution {

    @Override
    public AsyncExecutionState execute(ProcessContext context) {
        IncrementalAppInstanceUpdateConfiguration incrementalAppInstanceUpdateConfiguration = context.getVariable(Variables.INCREMENTAL_APP_INSTANCE_UPDATE_CONFIGURATION);
        CloudApplicationExtended appToProcess = context.getVariable(Variables.APP_TO_PROCESS);
        context.getStepLogger()
               .info("Desired application \"{0}\" instances {1} and now configured to {2}", appToProcess.getName(),
                     appToProcess.getInstances(), incrementalAppInstanceUpdateConfiguration.getNewApplicationInstanceCount());
        if (appToProcess.getInstances() == incrementalAppInstanceUpdateConfiguration.getNewApplicationInstanceCount()) {
            return AsyncExecutionState.FINISHED;
        }

        CloudApplication oldApplication = incrementalAppInstanceUpdateConfiguration.getOldApplication();
        CloudControllerClient client = context.getControllerClient();

        ImmutableIncrementalAppInstanceUpdateConfiguration.Builder incrementalAppInstanceUpdateConfigurationBuilder = ImmutableIncrementalAppInstanceUpdateConfiguration.builder()
                                                                                                                                                                        .from(incrementalAppInstanceUpdateConfiguration);
        if (oldApplication != null && incrementalAppInstanceUpdateConfiguration.getOldApplicationInstanceCount() > 1) {
            int oldApplicationInstancesCount = incrementalAppInstanceUpdateConfiguration.getOldApplicationInstanceCount() - 1;
            context.getStepLogger()
                   .info("Downscale application \"{0}\" to {1} instances", oldApplication.getName(), oldApplicationInstancesCount);
            client.updateApplicationInstances(oldApplication.getName(), oldApplicationInstancesCount);
            incrementalAppInstanceUpdateConfigurationBuilder.oldApplicationInstanceCount(oldApplicationInstancesCount);
        }

        CloudApplication newApplication = context.getVariable(Variables.EXISTING_APP_TO_POLL);
        int newApplicationInstancesCount = incrementalAppInstanceUpdateConfiguration.getNewApplicationInstanceCount() + 1;
        context.getStepLogger()
               .info("Upscale application \"{0}\" to {1} instances", newApplication.getName(), newApplicationInstancesCount);
        client.updateApplicationInstances(newApplication.getName(), newApplicationInstancesCount);
        incrementalAppInstanceUpdateConfigurationBuilder.newApplicationInstanceCount(newApplicationInstancesCount);
        context.setVariable(Variables.INCREMENTAL_APP_INSTANCE_UPDATE_CONFIGURATION,
                            incrementalAppInstanceUpdateConfigurationBuilder.build());

        context.setVariable(Variables.ASYNC_STEP_EXECUTION_INDEX, 0);
        return AsyncExecutionState.RUNNING;
    }

    @Override
    public String getPollingErrorMessage(ProcessContext context) {
        return MessageFormat.format("Error during poll of incremental instance update of module \"{0}\"",
                                    context.getVariable(Variables.APP_TO_PROCESS)
                                           .getModuleName());
    }

}
