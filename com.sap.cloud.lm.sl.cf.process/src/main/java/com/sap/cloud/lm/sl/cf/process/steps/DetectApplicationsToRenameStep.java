package com.sap.cloud.lm.sl.cf.process.steps;

import com.sap.cloud.lm.sl.cf.core.model.BlueGreenApplicationNameSuffix;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableDeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableDeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil;
import com.sap.cloud.lm.sl.cf.process.Messages;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import javax.inject.Named;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Named("detectApplicationsToRenameStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DetectApplicationsToRenameStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        DelegateExecution context = execution.getContext();
        //This is set here in case of the step returning early or failing because the call activity
        //following this step needs this variable, otherwise Flowable will throw an exception
        StepsUtil.setAppsToUndeploy(context, Collections.emptyList());
        if (!StepsUtil.getKeepOriginalAppNamesAfterDeploy(execution.getContext())) {
            return StepPhase.DONE;
        }
        DeployedMta deployedMta = StepsUtil.getDeployedMta(execution.getContext());
        if (deployedMta == null) {
            return StepPhase.DONE;
        }

        Set<String> deployedAppNames = CloudModelBuilderUtil.getDeployedApplicationNames(deployedMta.getApplications());
        List<String> appsToRename = computeOldAppsToRename(deployedAppNames);
        List<String> appsToUndeploy = computeLeftoverAppsToUndeploy(deployedAppNames);

        StepsUtil.setAppsToRename(context, appsToRename);
        setAppsToUndeploy(execution, appsToUndeploy);
        updateDeployedMta(context, deployedMta, appsToRename, appsToUndeploy);

        return StepPhase.DONE;
    }

    private List<String> computeOldAppsToRename(Set<String> appsToProcess) {
        return appsToProcess.stream()
                            .filter(appName -> !BlueGreenApplicationNameSuffix.isSuffixContainedIn(appName))
                            .collect(Collectors.toList());
    }

    private List<String> computeLeftoverAppsToUndeploy(Set<String> appsToProcess) {
        return appsToProcess.stream()
                            .filter(appName -> appName.endsWith(BlueGreenApplicationNameSuffix.LIVE.asSuffix()))
                            .filter(appName -> appsToProcess.contains(BlueGreenApplicationNameSuffix.removeSuffix(appName)))
                            .collect(Collectors.toList());
    }

    private void setAppsToUndeploy(ExecutionWrapper execution, List<String> appsToUndeploy) {
        CloudControllerClient client = execution.getControllerClient();
        List<CloudApplication> apps = appsToUndeploy.stream()
                                                    .map(app -> client.getApplication(app, false))
                                                    .filter(Objects::nonNull)
                                                    .collect(Collectors.toList());
        StepsUtil.setAppsToUndeploy(execution.getContext(), apps);
    }

    private void updateDeployedMta(DelegateExecution context, DeployedMta deployedMta, List<String> appsToUpdate,
                                   List<String> appsToUndeploy) {
        if (appsToUpdate.isEmpty()) {
            return;
        }
        List<DeployedMtaApplication> apps = deployedMta.getApplications();
        List<DeployedMtaApplication> updatedApps = apps.stream()
                                                       .filter(app -> !appsToUndeploy.contains(app.getName()))
                                                       .map(app -> updateDeployedAppNameIfNeeded(app, appsToUpdate))
                                                       .collect(Collectors.toList());
        StepsUtil.setDeployedMta(context, ImmutableDeployedMta.copyOf(deployedMta)
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
    protected String getStepErrorMessage(DelegateExecution context) {
        return Messages.ERROR_DETECTING_APPLICATIONS_TO_RENAME;
    }

}
