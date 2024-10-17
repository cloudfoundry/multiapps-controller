package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientFactory;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableIncrementalAppInstanceUpdateConfiguration;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.TimeoutType;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.InstanceInfo;
import com.sap.cloudfoundry.client.facade.domain.InstanceState;

import static org.cloudfoundry.multiapps.controller.process.steps.StepsUtil.disableAutoscaling;
import static org.cloudfoundry.multiapps.controller.process.steps.StepsUtil.enableAutoscaling;

@Named("incrementalAppInstancesUpdateStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class IncrementalAppInstancesUpdateStep extends TimeoutAsyncFlowableStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(IncrementalAppInstancesUpdateStep.class);
    private static final int MAX_TIMEOUT = (int) TimeUnit.HOURS.toSeconds(24);

    private final CloudControllerClientFactory clientFactory;
    private final TokenService tokenService;

    @Inject
    public IncrementalAppInstancesUpdateStep(CloudControllerClientFactory clientFactory, TokenService tokenService) {
        this.clientFactory = clientFactory;
        this.tokenService = tokenService;
    }

    @Override
    protected StepPhase executeAsyncStep(ProcessContext context) throws Exception {
        CloudApplicationExtended application = context.getVariable(Variables.APP_TO_PROCESS);
        DeployedMtaApplication oldApplication = getOldApplication(context, application);
        CloudControllerClient client = context.getControllerClient();
        try {
            if (oldApplication == null) {
                return scaleUpNewAppToTheRequiredInstances(context, application, client);
            }
            UUID oldApplicationGuid = client.getApplicationGuid(oldApplication.getName());
            disableAutoscaling(context, client, oldApplicationGuid);

            UUID applicationId = client.getApplicationGuid(application.getName());
            checkWhetherLiveAppNeedsPolling(context, client, oldApplication);
            context.getStepLogger()
                   .info(Messages.STARTING_INCREMENTAL_APPLICATION_INSTANCE_UPDATE_FOR_0, application.getName());
            List<InstanceInfo> idleApplicationInstances = client.getApplicationInstances(applicationId)
                                                                .getInstances();
            var incrementalAppInstanceUpdateConfigurationBuilder = ImmutableIncrementalAppInstanceUpdateConfiguration.builder()
                                                                                                                     .newApplication(application)
                                                                                                                     .newApplicationInstanceCount(idleApplicationInstances.size());

            int oldApplicationInstanceCount = client.getApplicationInstances(oldApplication)
                                                    .getInstances()
                                                    .size();
            incrementalAppInstanceUpdateConfigurationBuilder.oldApplication(oldApplication)
                                                            .oldApplicationInitialInstanceCount(oldApplicationInstanceCount)
                                                            .oldApplicationInstanceCount(oldApplicationInstanceCount);
            context.setVariable(Variables.INCREMENTAL_APP_INSTANCE_UPDATE_CONFIGURATION,
                                incrementalAppInstanceUpdateConfigurationBuilder.build());
            return checkWhetherNewAppIsAlreadyScaled(context, idleApplicationInstances, application, client);
        } catch (Exception e) {
            if (oldApplication != null) {
                enableAutoscaling(client, oldApplication);
            }
            throw e;
        }
    }

    private StepPhase scaleUpNewAppToTheRequiredInstances(ProcessContext context, CloudApplicationExtended application,
                                                          CloudControllerClient client) {
        ImmutableIncrementalAppInstanceUpdateConfiguration.Builder incrementalAppInstanceUpdateConfigurationBuilder;
        context.getStepLogger()
               .info(Messages.DUE_TO_MISSING_PRODUCTIVE_DEPLOYED_APPLICATION_OF_MODULE_0_THE_NEW_APPLICATION_WILL_BE_SCALED_IN_STANDARD_WAY,
                     application.getModuleName());
        client.updateApplicationInstances(application.getName(), application.getInstances());
        incrementalAppInstanceUpdateConfigurationBuilder = ImmutableIncrementalAppInstanceUpdateConfiguration.builder()
                                                                                                             .newApplication(application)
                                                                                                             .newApplicationInstanceCount(application.getInstances());
        context.setVariable(Variables.INCREMENTAL_APP_INSTANCE_UPDATE_CONFIGURATION,
                            incrementalAppInstanceUpdateConfigurationBuilder.build());
        setExecutionIndexForPollingNewAppInstances(context);
        return StepPhase.POLL;
    }

    private DeployedMtaApplication getOldApplication(ProcessContext context, CloudApplicationExtended currentApplication) {
        DeployedMta deployedMta = context.getVariable(Variables.DEPLOYED_MTA);
        if (deployedMta == null) {
            LOGGER.info(Messages.NO_DEPLOYED_MTA_DETECTED_DURING_ROLLING_INSTANCE_UPDATE);
            return null;
        }
        if (deployedMta.getApplications() == null) {
            LOGGER.info(Messages.LIVE_APPLICATION_NOT_DETECTED_DURING_ROLLING_INSTANCE_UPDATE);
            return null;
        }
        DeployedMtaApplication deployedMtaApplication = deployedMta.getApplications()
                                                                   .stream()
                                                                   .filter(deployedApplication -> deployedApplication.getModuleName()
                                                                                                                     .equals(currentApplication.getModuleName()))
                                                                   .findFirst()
                                                                   .orElse(null);
        if (deployedMtaApplication == null) {
            LOGGER.info(Messages.THE_REQUIRED_APPLICATION_NOT_FOUND_IN_THE_DETECTED_MTA);
            return null;
        }
        if (deployedMtaApplication.getName()
                                  .equals(currentApplication.getName())) {
            context.getStepLogger()
                   .debug(Messages.THE_DETECTED_APPLICATION_HAS_THE_SAME_NAME_AS_THE_NEW_ONE);
            return null;
        }
        return deployedMtaApplication;
    }

    private void checkWhetherLiveAppNeedsPolling(ProcessContext context, CloudControllerClient client, CloudApplication cloudApplication) {
        setExecutionIndexToTriggerNewApplicationRollingUpdate(context);
        List<InstanceInfo> appInstances = client.getApplicationInstances(cloudApplication)
                                                .getInstances();
        if (!appInstances.stream()
                         .allMatch(instanceInfo -> instanceInfo.getState() == InstanceState.RUNNING)) {
            context.getStepLogger()
                   .debug(Messages.NOT_ALL_OF_THE_APPLICATION_0_INSTANCES_ARE_RUNNING_WAITING_FOR_ALL_INSTANCES_TO_START,
                          cloudApplication.getName());
            setExecutionIndexForPollingLiveApplicationInstances(context);
        }
    }

    private StepPhase checkWhetherNewAppIsAlreadyScaled(ProcessContext context, List<InstanceInfo> idleApplicationInstances,
                                                        CloudApplicationExtended application, CloudControllerClient client) {
        if (idleApplicationInstances.size() >= application.getInstances()) {
            context.getStepLogger()
                   .info(Messages.APPLICATION_0_ALREADY_SCALED_TO_THE_DESIRED_1_INSTANCES, application.getName(),
                         application.getInstances());
            enableAutoscaling(client, application);
            return StepPhase.DONE;
        }
        return StepPhase.POLL;
    }

    private void setExecutionIndexForPollingLiveApplicationInstances(ProcessContext context) {
        context.setVariable(Variables.ASYNC_STEP_EXECUTION_INDEX, 0);
    }

    private void setExecutionIndexForPollingNewAppInstances(ProcessContext context) {
        context.setVariable(Variables.ASYNC_STEP_EXECUTION_INDEX, 1);
    }

    private void setExecutionIndexToTriggerNewApplicationRollingUpdate(ProcessContext context) {
        context.setVariable(Variables.ASYNC_STEP_EXECUTION_INDEX, 2);
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ProcessContext context) {
        // The sequence of executions is crucial, as the incremental blue-green deployment alternates between them during the polling
        // process
        return List.of(new PollStartLiveAppExecution(clientFactory, tokenService),
                       new PollStartAppExecutionWithRollbackExecution(clientFactory, tokenService),
                       new PollIncrementalAppInstanceUpdateExecution());
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return MessageFormat.format(Messages.ERROR_DURING_INCREMENTAL_INSTANCE_UPDATE_OF_MODULE_0,
                                    context.getVariable(Variables.APP_TO_PROCESS)
                                           .getModuleName());
    }

    @Override
    public Duration getTimeout(ProcessContext context) {
        CloudApplicationExtended application = context.getVariable(Variables.APP_TO_PROCESS);
        Duration timeout = calculateTimeout(context, TimeoutType.START);
        return Duration.ofSeconds(Math.min(timeout.getSeconds() * application.getInstances(), MAX_TIMEOUT));
    }

}
