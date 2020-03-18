package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.variable.api.delegate.VariableScope;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationNameSuffixAppender;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.model.BlueGreenApplicationNameSuffix;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableDeployedMta;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ApplicationColorDetector;
import com.sap.cloud.lm.sl.cf.process.util.ApplicationProductizationStateUpdater;
import com.sap.cloud.lm.sl.cf.process.util.ApplicationProductizationStateUpdaterBasedOnAge;
import com.sap.cloud.lm.sl.cf.process.util.ApplicationProductizationStateUpdaterBasedOnColor;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;

@Named("renameApplicationsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RenameApplicationsStep extends SyncFlowableStep {

    @Inject
    private ApplicationColorDetector applicationColorDetector;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        RenameFlow renameFlow = createFlow(execution.getContext());
        renameFlow.execute(execution);
        return StepPhase.DONE;
    }

    private RenameFlow createFlow(VariableScope context) {
        if (StepsUtil.getKeepOriginalAppNamesAfterDeploy(context)) {
            return new RenameApplicationsWithOldNewSuffix();
        }
        return new RenameApplicationsWithBlueGreenSuffix();
    }

    @Override
    protected String getStepErrorMessage(ExecutionWrapper execution) {
        return Messages.ERROR_RENAMING_APPLICATIONS;
    }

    interface RenameFlow {
        void execute(ExecutionWrapper execution);
    }

    class RenameApplicationsWithOldNewSuffix implements RenameFlow {

        @Override
        public void execute(ExecutionWrapper execution) {
            List<String> appsToRename = execution.getVariable(Variables.APPS_TO_RENAME);
            if (appsToRename != null) {
                renameOldApps(appsToRename, execution.getControllerClient());
            }

            DeployedMta deployedMta = execution.getVariable(Variables.DEPLOYED_MTA);
            if (deployedMta != null) {
                ApplicationProductizationStateUpdater appUpdater = new ApplicationProductizationStateUpdaterBasedOnAge(getStepLogger());
                setIdleApplications(execution, deployedMta, appUpdater);
            }

            getStepLogger().debug(Messages.UPDATING_APP_NAMES_WITH_NEW_SUFFIX);
            updateApplicationNamesInDescriptor(execution, BlueGreenApplicationNameSuffix.IDLE.asSuffix());
        }

        private void renameOldApps(List<String> appsToRename, CloudControllerClient client) {
            for (String appName : appsToRename) {
                String newName = appName + BlueGreenApplicationNameSuffix.LIVE.asSuffix();
                getStepLogger().info(Messages.RENAMING_APPLICATION_0_TO_1, appName, newName);
                client.rename(appName, newName);
            }
        }

    }

    class RenameApplicationsWithBlueGreenSuffix implements RenameFlow {

        private final ApplicationColor DEFAULT_MTA_COLOR = ApplicationColor.BLUE;

        @Override
        public void execute(ExecutionWrapper execution) {
            DelegateExecution context = execution.getContext();
            getStepLogger().debug(Messages.DETECTING_COLOR_OF_DEPLOYED_MTA);
            DeployedMta deployedMta = execution.getVariable(Variables.DEPLOYED_MTA);
            ApplicationColor idleMtaColor = DEFAULT_MTA_COLOR;

            if (deployedMta == null) {
                getStepLogger().info(Messages.NEW_MTA_COLOR, idleMtaColor);
                StepsUtil.setIdleMtaColor(context, idleMtaColor);
                updateApplicationNamesInDescriptor(execution, idleMtaColor.asSuffix());
                return;
            }

            ApplicationColor liveMtaColor = computeLiveMtaColor(context, deployedMta);
            if (liveMtaColor != null) {
                idleMtaColor = liveMtaColor.getAlternativeColor();
            }
            getStepLogger().info(Messages.NEW_MTA_COLOR, idleMtaColor);

            ApplicationProductizationStateUpdater appUpdater = new ApplicationProductizationStateUpdaterBasedOnColor(getStepLogger(),
                                                                                                                     liveMtaColor);
            setIdleApplications(execution, deployedMta, appUpdater);
            StepsUtil.setLiveMtaColor(context, liveMtaColor);
            StepsUtil.setIdleMtaColor(context, idleMtaColor);
            updateApplicationNamesInDescriptor(execution, idleMtaColor.asSuffix());
        }

        private ApplicationColor computeLiveMtaColor(DelegateExecution context, DeployedMta deployedMta) {
            try {
                ApplicationColor liveMtaColor = applicationColorDetector.detectSingularDeployedApplicationColor(deployedMta);
                getStepLogger().info(Messages.DEPLOYED_MTA_COLOR, liveMtaColor);
                return liveMtaColor;
            } catch (ConflictException e) {
                getStepLogger().warn(e.getMessage());
                ApplicationColor liveMtaColor = applicationColorDetector.detectLiveApplicationColor(deployedMta,
                                                                                                    StepsUtil.getCorrelationId(context));
                ApplicationColor idleMtaColor = liveMtaColor.getAlternativeColor();
                getStepLogger().info(Messages.ASSUMED_LIVE_AND_IDLE_COLORS, liveMtaColor, idleMtaColor);
                return liveMtaColor;
            }
        }

    }

    private void setIdleApplications(ExecutionWrapper execution, DeployedMta deployedMta, ApplicationProductizationStateUpdater appUpdater) {
        List<DeployedMtaApplication> updatedApplications = appUpdater.updateApplicationsProductizationState(deployedMta.getApplications());
        execution.setVariable(Variables.DEPLOYED_MTA, ImmutableDeployedMta.copyOf(deployedMta)
                                                                          .withApplications(updatedApplications));
    }

    private void updateApplicationNamesInDescriptor(ExecutionWrapper execution, String suffix) {
        DeploymentDescriptor descriptor = execution.getVariable(Variables.DEPLOYMENT_DESCRIPTOR);
        ApplicationNameSuffixAppender appender = new ApplicationNameSuffixAppender(suffix);
        descriptor.accept(appender);
        execution.setVariable(Variables.DEPLOYMENT_DESCRIPTOR, descriptor);
    }

}
