package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.common.ConflictException;
import org.cloudfoundry.multiapps.controller.core.helpers.ApplicationNameSuffixAppender;
import org.cloudfoundry.multiapps.controller.core.model.ApplicationColor;
import org.cloudfoundry.multiapps.controller.core.model.BlueGreenApplicationNameSuffix;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMta;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationSubscription;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationSubscriptionService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ApplicationColorDetector;
import org.cloudfoundry.multiapps.controller.process.util.ApplicationProductizationStateUpdater;
import org.cloudfoundry.multiapps.controller.process.util.ApplicationProductizationStateUpdaterBasedOnAge;
import org.cloudfoundry.multiapps.controller.process.util.ApplicationProductizationStateUpdaterBasedOnColor;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;

@Named("renameApplicationsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RenameApplicationsStep extends SyncFlowableStep {

    @Inject
    private ApplicationColorDetector applicationColorDetector;
    @Inject
    private ConfigurationSubscriptionService subscriptionService;

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        RenameFlow renameFlow = createFlow(context);
        renameFlow.execute(context);
        return StepPhase.DONE;
    }

    private RenameFlow createFlow(ProcessContext context) {
        if (context.getVariable(Variables.KEEP_ORIGINAL_APP_NAMES_AFTER_DEPLOY)) {
            return new RenameApplicationsWithOldNewSuffix();
        }
        return new RenameApplicationsWithBlueGreenSuffix();
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_RENAMING_APPLICATIONS;
    }

    interface RenameFlow {
        void execute(ProcessContext context);
    }

    class RenameApplicationsWithOldNewSuffix implements RenameFlow {

        @Override
        public void execute(ProcessContext context) {
            List<String> appsToRename = context.getVariable(Variables.APPS_TO_RENAME);
            if (appsToRename != null) {
                renameOldApps(appsToRename, context.getControllerClient());
                String mtaId = context.getVariable(Variables.MTA_ID);
                String spaceId = context.getVariable(Variables.SPACE_GUID);
                updateConfigurationSubscribers(appsToRename, mtaId, spaceId);
            }

            DeployedMta deployedMta = context.getVariable(Variables.DEPLOYED_MTA);
            if (deployedMta != null) {
                ApplicationProductizationStateUpdater appUpdater = new ApplicationProductizationStateUpdaterBasedOnAge(getStepLogger());
                setIdleApplications(context, deployedMta, appUpdater);
            }

            getStepLogger().debug(Messages.UPDATING_APP_NAMES_WITH_NEW_SUFFIX);
            updateApplicationNamesInDescriptor(context, BlueGreenApplicationNameSuffix.IDLE.asSuffix());
        }

        private void renameOldApps(List<String> appsToRename, CloudControllerClient client) {
            for (String appName : appsToRename) {
                String newName = appName + BlueGreenApplicationNameSuffix.LIVE.asSuffix();
                getStepLogger().info(Messages.RENAMING_APPLICATION_0_TO_1, appName, newName);
                client.rename(appName, newName);
            }
        }

        private void updateConfigurationSubscribers(List<String> appsToRename, String mtaId, String spaceId) {
            List<ConfigurationSubscription> subscriptions = subscriptionService.createQuery()
                                                                               .mtaId(mtaId)
                                                                               .spaceId(spaceId)
                                                                               .list();
            for (ConfigurationSubscription subscription : subscriptions) {
                if (appsToRename.contains(subscription.getAppName())) {
                    String newAppName = subscription.getAppName() + BlueGreenApplicationNameSuffix.LIVE.asSuffix();
                    getStepLogger().debug(Messages.UPDATING_CONFIGURATION_SUBSCRIPTION_0_WITH_NAME_1, subscription.getAppName(), newAppName);
                    updateConfigurationSubscription(subscription, newAppName);
                }
            }
        }

        private void updateConfigurationSubscription(ConfigurationSubscription subscription, String newAppName) {
            ConfigurationSubscription newSubscription = createNewSubscription(subscription, newAppName);
            subscriptionService.update(subscription, newSubscription);
        }

        private ConfigurationSubscription createNewSubscription(ConfigurationSubscription subscription, String newAppName) {
            return new ConfigurationSubscription(subscription.getId(),
                                                 subscription.getMtaId(),
                                                 subscription.getSpaceId(),
                                                 newAppName,
                                                 subscription.getFilter(),
                                                 subscription.getModuleDto(),
                                                 subscription.getResourceDto(),
                                                 subscription.getModuleId(),
                                                 subscription.getResourceId());
        }
    }

    class RenameApplicationsWithBlueGreenSuffix implements RenameFlow {

        private final ApplicationColor defaultMtaColor = ApplicationColor.BLUE;

        @Override
        public void execute(ProcessContext context) {
            getStepLogger().debug(Messages.DETECTING_COLOR_OF_DEPLOYED_MTA);
            DeployedMta deployedMta = context.getVariable(Variables.DEPLOYED_MTA);
            ApplicationColor idleMtaColor = defaultMtaColor;

            if (deployedMta == null) {
                getStepLogger().info(Messages.NEW_MTA_COLOR, idleMtaColor);
                context.setVariable(Variables.IDLE_MTA_COLOR, idleMtaColor);
                updateApplicationNamesInDescriptor(context, idleMtaColor.asSuffix());
                return;
            }

            ApplicationColor liveMtaColor = computeLiveMtaColor(context, deployedMta);
            if (liveMtaColor != null) {
                idleMtaColor = liveMtaColor.getAlternativeColor();
            }
            getStepLogger().info(Messages.NEW_MTA_COLOR, idleMtaColor);

            ApplicationProductizationStateUpdater appUpdater = new ApplicationProductizationStateUpdaterBasedOnColor(getStepLogger(),
                                                                                                                     liveMtaColor);
            setIdleApplications(context, deployedMta, appUpdater);
            context.setVariable(Variables.LIVE_MTA_COLOR, liveMtaColor);
            context.setVariable(Variables.IDLE_MTA_COLOR, idleMtaColor);
            updateApplicationNamesInDescriptor(context, idleMtaColor.asSuffix());
        }

        private ApplicationColor computeLiveMtaColor(ProcessContext context, DeployedMta deployedMta) {
            try {
                ApplicationColor liveMtaColor = applicationColorDetector.detectSingularDeployedApplicationColor(deployedMta);
                getStepLogger().info(Messages.DEPLOYED_MTA_COLOR, liveMtaColor);
                return liveMtaColor;
            } catch (ConflictException e) {
                getStepLogger().warn(e.getMessage());
                ApplicationColor liveMtaColor = applicationColorDetector.detectLiveApplicationColor(deployedMta,
                                                                                                    context.getVariable(Variables.CORRELATION_ID));
                ApplicationColor idleMtaColor = liveMtaColor.getAlternativeColor();
                getStepLogger().info(Messages.ASSUMED_LIVE_AND_IDLE_COLORS, liveMtaColor, idleMtaColor);
                return liveMtaColor;
            }
        }

    }

    private void setIdleApplications(ProcessContext context, DeployedMta deployedMta, ApplicationProductizationStateUpdater appUpdater) {
        List<DeployedMtaApplication> updatedApplications = appUpdater.updateApplicationsProductizationState(deployedMta.getApplications());
        context.setVariable(Variables.DEPLOYED_MTA, ImmutableDeployedMta.copyOf(deployedMta)
                                                                        .withApplications(updatedApplications));
    }

    private void updateApplicationNamesInDescriptor(ProcessContext context, String suffix) {
        DeploymentDescriptor descriptor = context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR);
        ApplicationNameSuffixAppender appender = new ApplicationNameSuffixAppender(suffix);
        descriptor.accept(appender);
        context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR, descriptor);
    }

}
