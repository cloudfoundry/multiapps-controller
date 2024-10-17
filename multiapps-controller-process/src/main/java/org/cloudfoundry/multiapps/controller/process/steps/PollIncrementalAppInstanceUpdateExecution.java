package org.cloudfoundry.multiapps.controller.process.steps;

import static org.cloudfoundry.multiapps.controller.process.steps.StepsUtil.enableAutoscaling;

import java.text.MessageFormat;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableIncrementalAppInstanceUpdateConfiguration;
import org.cloudfoundry.multiapps.controller.core.model.IncrementalAppInstanceUpdateConfiguration;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;

public class PollIncrementalAppInstanceUpdateExecution implements AsyncExecution {

    @Override
    public AsyncExecutionState execute(ProcessContext context) {
        CloudApplicationExtended appToProcess = context.getVariable(Variables.APP_TO_PROCESS);
        CloudControllerClient client = context.getControllerClient();
        IncrementalAppInstanceUpdateConfiguration incrementalAppInstanceUpdateConfiguration = context.getVariable(Variables.INCREMENTAL_APP_INSTANCE_UPDATE_CONFIGURATION);
        context.getStepLogger()
               .debug(Messages.DESIRED_APPLICATION_0_INSTANCES_1_AND_NOW_SCALED_TO_2, appToProcess.getName(), appToProcess.getInstances(),
                      incrementalAppInstanceUpdateConfiguration.getNewApplicationInstanceCount());
        if (incrementalAppInstanceUpdateConfiguration.getNewApplicationInstanceCount() >= appToProcess.getInstances()) {
            enableAutoscaling(client, appToProcess);
            return AsyncExecutionState.FINISHED;
        }
        return rescaleApplications(context, incrementalAppInstanceUpdateConfiguration, client);
    }

    private AsyncExecutionState rescaleApplications(ProcessContext context,
                                                    IncrementalAppInstanceUpdateConfiguration incrementalAppInstanceUpdateConfiguration,
                                                    CloudControllerClient client) {
        var updatedIncrementalAppInstanceUpdateConfiguration = downscaleOldApplication(context, incrementalAppInstanceUpdateConfiguration,
                                                                                       client);
        updatedIncrementalAppInstanceUpdateConfiguration = scaleUpNewApplication(context, updatedIncrementalAppInstanceUpdateConfiguration,
                                                                                 client);
        context.setVariable(Variables.INCREMENTAL_APP_INSTANCE_UPDATE_CONFIGURATION, updatedIncrementalAppInstanceUpdateConfiguration);
        updateAsyncExecutionIndexes(context);
        return AsyncExecutionState.RUNNING;
    }

    private void updateAsyncExecutionIndexes(ProcessContext context) {
        CloudApplication existingAppToPoll = context.getVariable(Variables.EXISTING_APP_TO_POLL);
        if (existingAppToPoll == null) {
            existingAppToPoll = context.getControllerClient()
                                       .getApplication(context.getVariable(Variables.APP_TO_PROCESS)
                                                              .getName());
            context.setVariable(Variables.EXISTING_APP_TO_POLL, existingAppToPoll);
        }
        if (existingAppToPoll.getState()
                             .equals(CloudApplication.State.STOPPED)) {
            continueWithIncrementalScaling(context);
        } else {
            pollApplicationIndexesBeforeContinueingWithIncrementalScaling(context);
        }
    }

    private void continueWithIncrementalScaling(ProcessContext context) {
        context.setVariable(Variables.ASYNC_STEP_EXECUTION_INDEX, 2);
    }

    private void pollApplicationIndexesBeforeContinueingWithIncrementalScaling(ProcessContext context) {
        context.setVariable(Variables.ASYNC_STEP_EXECUTION_INDEX, 1);
    }

    private IncrementalAppInstanceUpdateConfiguration
            downscaleOldApplication(ProcessContext context,
                                    IncrementalAppInstanceUpdateConfiguration incrementalAppInstanceUpdateConfiguration,
                                    CloudControllerClient client) {
        var incrementalAppInstanceUpdateConfigurationBuilder = ImmutableIncrementalAppInstanceUpdateConfiguration.builder()
                                                                                                                 .from(incrementalAppInstanceUpdateConfiguration);
        CloudApplication oldApplication = incrementalAppInstanceUpdateConfiguration.getOldApplication();
        if (oldApplication != null && incrementalAppInstanceUpdateConfiguration.getOldApplicationInstanceCount() > 1) {
            int oldApplicationInstancesCount = incrementalAppInstanceUpdateConfiguration.getOldApplicationInstanceCount() - 1;
            context.getStepLogger()
                   .debug(Messages.DOWNSCALING_APPLICATION_0_TO_1_INSTANCES, oldApplication.getName(), oldApplicationInstancesCount);
            client.updateApplicationInstances(oldApplication.getName(), oldApplicationInstancesCount);
            incrementalAppInstanceUpdateConfigurationBuilder.oldApplicationInstanceCount(oldApplicationInstancesCount);
        }
        return incrementalAppInstanceUpdateConfigurationBuilder.build();
    }

    private IncrementalAppInstanceUpdateConfiguration
            scaleUpNewApplication(ProcessContext context,
                                  IncrementalAppInstanceUpdateConfiguration incrementalAppInstanceUpdateConfiguration,
                                  CloudControllerClient client) {
        var incrementalAppInstanceUpdateConfigurationBuilder = ImmutableIncrementalAppInstanceUpdateConfiguration.builder()
                                                                                                                 .from(incrementalAppInstanceUpdateConfiguration);
        CloudApplication newApplication = context.getVariable(Variables.APP_TO_PROCESS);
        int newApplicationInstancesCount = incrementalAppInstanceUpdateConfiguration.getNewApplicationInstanceCount() + 1;
        context.getStepLogger()
               .debug(Messages.UPSCALING_APPLICATION_0_TO_1_INSTANCES, newApplication.getName(), newApplicationInstancesCount);
        client.updateApplicationInstances(newApplication.getName(), newApplicationInstancesCount);
        incrementalAppInstanceUpdateConfigurationBuilder.newApplicationInstanceCount(newApplicationInstancesCount);
        return incrementalAppInstanceUpdateConfigurationBuilder.build();
    }

    @Override
    public String getPollingErrorMessage(ProcessContext context) {
        return MessageFormat.format(Messages.ERROR_DURING_POLL_OF_INCREMENTAL_INSTANCE_UPDATE_OF_MODULE_0,
                                    context.getVariable(Variables.APP_TO_PROCESS)
                                           .getModuleName());
    }

}
