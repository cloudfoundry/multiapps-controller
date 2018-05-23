package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.activiti.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.ApplicationsCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.ConfigurationEntriesCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;

@Component("rebuildApplicationDeployModelStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RebuildApplicationDeployModelStep extends SyncActivitiStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws Exception {
        try {
            CloudApplicationExtended app = StepsUtil.getApp(execution.getContext());
            getStepLogger().info(Messages.BUILDING_CLOUD_APP_MODEL, app.getName());

            DeployedMta deployedMta = StepsUtil.getDeployedMta(execution.getContext());
            List<DeployedMtaModule> deployedModules = (deployedMta != null) ? deployedMta.getModules() : Collections.emptyList();
            Set<String> mtaArchiveModules = StepsUtil.getMtaArchiveModules(execution.getContext());
            getStepLogger().debug(Messages.MTA_ARCHIVE_MODULES, mtaArchiveModules);
            Set<String> deployedModuleNames = CloudModelBuilderUtil.getDeployedModuleNames(deployedModules);
            getStepLogger().debug(Messages.DEPLOYED_MODULES, deployedModuleNames);
            Set<String> mtaModules = StepsUtil.getMtaModules(execution.getContext());
            getStepLogger().debug(Messages.MTA_MODULES, mtaModules);
            // TODO: Build a cloud model only for the current application:
            List<CloudApplicationExtended> modifiedApps = getApplicationsCloudModelBuilder(execution.getContext()).build(mtaArchiveModules,
                mtaModules, deployedModuleNames);
            CloudApplicationExtended modifiedApp = findApplication(modifiedApps, app.getName());
            setApplicationUris(execution.getContext(), app, modifiedApp);
            app.setIdleUris(modifiedApp.getIdleUris());
            app.setEnv(MapUtil.upcastUnmodifiable(modifiedApp.getEnvAsMap()));
            getStepLogger().debug(Messages.APP_WITH_UPDATED_ENVIRONMENT, JsonUtil.toJson(app, true));
            StepsUtil.setApp(execution.getContext(), app);

            buildConfigurationEntries(execution.getContext(), app);

            getStepLogger().debug(Messages.CLOUD_APP_MODEL_BUILT);
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_BUILDING_CLOUD_APP_MODEL);
            throw e;
        }
        return StepPhase.DONE;
    }

    private void setApplicationUris(DelegateExecution context, CloudApplicationExtended app, CloudApplicationExtended modifiedApp) {
        if (StepsUtil.getUseIdleUris(context)) {
            app.setUris(modifiedApp.getIdleUris());
        } else {
            app.setUris(modifiedApp.getUris());
        }
    }

    private void buildConfigurationEntries(DelegateExecution context, CloudApplicationExtended app) {
        if (StepsUtil.getSkipUpdateConfigurationEntries(context)) {
            StepsUtil.setConfigurationEntriesToPublish(context, Collections.emptyList());
            return;
        }
        DeploymentDescriptor deploymentDescriptor = StepsUtil.getDeploymentDescriptor(context);

        ConfigurationEntriesCloudModelBuilder configurationEntriesCloudModelBuilder = getConfigurationEntriesCloudModelBuilder(context);
        Map<String, List<ConfigurationEntry>> allConfigurationEntries = configurationEntriesCloudModelBuilder.build(deploymentDescriptor);
        List<ConfigurationEntry> updatedModuleNames = allConfigurationEntries.getOrDefault(app.getModuleName(), Collections.emptyList());
        StepsUtil.setConfigurationEntriesToPublish(context, updatedModuleNames);
        StepsUtil.setSkipUpdateConfigurationEntries(context, false);

        getStepLogger().debug(Messages.CONFIGURATION_ENTRIES_TO_PUBLISH, JsonUtil.toJson(updatedModuleNames, true));
    }

    private ApplicationsCloudModelBuilder getApplicationsCloudModelBuilder(DelegateExecution context) {
        return StepsUtil.getApplicationsCloudModelBuilder(context);
    }

    private ConfigurationEntriesCloudModelBuilder getConfigurationEntriesCloudModelBuilder(DelegateExecution context) {
        String orgName = StepsUtil.getOrg(context);
        String spaceName = StepsUtil.getSpace(context);
        String spaceId = StepsUtil.getSpaceId(context);
        return new ConfigurationEntriesCloudModelBuilder(orgName, spaceName, spaceId);
    }

    private CloudApplicationExtended findApplication(List<CloudApplicationExtended> apps, String applicationName) {
        return apps.stream()
            .filter(app -> app.getName()
                .equals(applicationName))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                MessageFormat.format(Messages.APPLICATION_NOT_FOUND_IN_REBUILT_CLOUD_MODEL, applicationName)));
    }

}
