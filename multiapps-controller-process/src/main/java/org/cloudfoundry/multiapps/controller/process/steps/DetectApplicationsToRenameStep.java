package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.apache.commons.collections4.CollectionUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.core.model.BlueGreenApplicationNameSuffix;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.util.CloudModelBuilderUtil;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("detectApplicationsToRenameStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DetectApplicationsToRenameStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        // This is set here in case of the step returning early or failing because the call activity
        // following this step needs this variable, otherwise Flowable will throw an exception
        context.setVariable(Variables.APPS_TO_UNDEPLOY, Collections.emptyList());
        if (!context.getVariable(Variables.KEEP_ORIGINAL_APP_NAMES_AFTER_DEPLOY)) {
            return StepPhase.DONE;
        }
        DeployedMta deployedMta = context.getVariable(Variables.DEPLOYED_MTA);
        if (deployedMta == null) {
            return StepPhase.DONE;
        }
        List<String> selectedModules = context.getVariable(Variables.MODULES_FOR_DEPLOYMENT);

        List<String> appsToRename = computeOldAppsToRename(deployedMta, selectedModules);
        List<String> appsToUndeploy = computeLeftoverAppsToUndeploy(deployedMta);

        context.setVariable(Variables.APPS_TO_RENAME, appsToRename);
        setAppsToUndeploy(context, appsToUndeploy);
        updateDeployedMta(context, deployedMta, appsToRename, appsToUndeploy);

        return StepPhase.DONE;
    }

    private List<String> computeOldAppsToRename(DeployedMta deployedMta, List<String> selectedModules) {
        return deployedMta.getApplications()
                          .stream()
                          .filter(app -> !BlueGreenApplicationNameSuffix.isSuffixContainedIn(app.getName()))
                          .filter(app -> isModuleSelectedForDeployment(app.getModuleName(), selectedModules))
                          .map(DeployedMtaApplication::getName)
                          .collect(Collectors.toList());
    }

    private boolean isModuleSelectedForDeployment(String moduleName, List<String> selectedModules) {
        if (CollectionUtils.isEmpty(selectedModules)) {
            return true;
        }

        return selectedModules.contains(moduleName);
    }

    private List<String> computeLeftoverAppsToUndeploy(DeployedMta deployedMta) {
        Set<String> appsToProcess = CloudModelBuilderUtil.getDeployedApplicationNames(deployedMta.getApplications());
        return appsToProcess.stream()
                            .filter(appName -> appName.endsWith(BlueGreenApplicationNameSuffix.LIVE.asSuffix()))
                            .filter(appName -> appsToProcess.contains(BlueGreenApplicationNameSuffix.removeSuffix(appName)))
                            .collect(Collectors.toList());
    }

    private void setAppsToUndeploy(ProcessContext context, List<String> appsToUndeploy) {
        CloudControllerClient client = context.getControllerClient();
        List<CloudApplication> apps = appsToUndeploy.stream()
                                                    .map(app -> client.getApplication(app, false))
                                                    .filter(Objects::nonNull)
                                                    .collect(Collectors.toList());
        context.setVariable(Variables.APPS_TO_UNDEPLOY, apps);
    }

    private void updateDeployedMta(ProcessContext context, DeployedMta deployedMta, List<String> appsToUpdate,
                                   List<String> appsToUndeploy) {
        if (appsToUpdate.isEmpty()) {
            return;
        }
        List<DeployedMtaApplication> apps = deployedMta.getApplications();
        List<DeployedMtaApplication> updatedApps = apps.stream()
                                                       .filter(app -> !appsToUndeploy.contains(app.getName()))
                                                       .map(app -> updateDeployedAppNameIfNeeded(app, appsToUpdate))
                                                       .collect(Collectors.toList());
        context.setVariable(Variables.DEPLOYED_MTA, ImmutableDeployedMta.copyOf(deployedMta)
                                                                        .withApplications(updatedApps));
    }

    private DeployedMtaApplication updateDeployedAppNameIfNeeded(DeployedMtaApplication app, List<String> appsToUpdate) {
        if (appsToUpdate.contains(app.getName())) {
            return ImmutableDeployedMtaApplication.copyOf(app)
                                                  .withName(app.getName() + BlueGreenApplicationNameSuffix.LIVE.asSuffix());
        }
        return app;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_DETECTING_APPLICATIONS_TO_RENAME;
    }

}
