package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ApplicationCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ConfigurationEntriesCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.helpers.ModuleToDeployHelper;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Module;

public class BuildApplicationDeployModelStep extends SyncFlowableStep {

    @Inject
    private ModuleToDeployHelper moduleToDeployHelper;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws Exception {
        try {
            Module module = StepsUtil.getModuleToDeploy(execution.getContext());
            getStepLogger().debug(Messages.BUILDING_CLOUD_APP_MODEL, module.getName());

            Module applicationModule = StepsUtil.findModuleInDeploymentDescriptor(execution.getContext(), module.getName());
            StepsUtil.setModuleToDeploy(execution.getContext(), applicationModule);
            CloudApplicationExtended modifiedApp = getApplicationCloudModelBuilder(execution.getContext()).build(applicationModule,
                moduleToDeployHelper);
            editUrisIfIdleApplication(execution.getContext(), modifiedApp);
            modifiedApp.setEnv(getApplicationEnv(execution.getContext(), modifiedApp));
            SecureSerializationFacade secureSerializationFacade = new SecureSerializationFacade();
            String appJson = secureSerializationFacade.toJson(modifiedApp);
            getStepLogger().debug(Messages.APP_WITH_UPDATED_ENVIRONMENT, appJson);
            StepsUtil.setApp(execution.getContext(), modifiedApp);

            buildConfigurationEntries(execution.getContext(), modifiedApp);

            getStepLogger().debug(Messages.CLOUD_APP_MODEL_BUILT);
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_BUILDING_CLOUD_APP_MODEL);
            throw e;
        }
        return StepPhase.DONE;
    }

    protected Map<Object, Object> getApplicationEnv(DelegateExecution context, CloudApplicationExtended app) {
        return MapUtil.upcastUnmodifiable(app.getEnvAsMap());
    }

    private void editUrisIfIdleApplication(DelegateExecution context, CloudApplicationExtended modifiedApp) {
        if (StepsUtil.getUseIdleUris(context)) {
            modifiedApp.setUris(modifiedApp.getIdleUris());
        }
    }

    private void buildConfigurationEntries(DelegateExecution context, CloudApplicationExtended app) {
        if (StepsUtil.getSkipUpdateConfigurationEntries(context)) {
            StepsUtil.setConfigurationEntriesToPublish(context, Collections.emptyList());
            return;
        }
        DeploymentDescriptor deploymentDescriptor = StepsUtil.getCompleteDeploymentDescriptor(context);

        ConfigurationEntriesCloudModelBuilder configurationEntriesCloudModelBuilder = getConfigurationEntriesCloudModelBuilder(context);
        Map<String, List<ConfigurationEntry>> allConfigurationEntries = configurationEntriesCloudModelBuilder.build(deploymentDescriptor);
        List<ConfigurationEntry> updatedModuleNames = allConfigurationEntries.getOrDefault(app.getModuleName(), Collections.emptyList());
        StepsUtil.setConfigurationEntriesToPublish(context, updatedModuleNames);
        StepsUtil.setSkipUpdateConfigurationEntries(context, false);

        getStepLogger().debug(Messages.CONFIGURATION_ENTRIES_TO_PUBLISH, JsonUtil.toJson(updatedModuleNames, true));
    }

    private ApplicationCloudModelBuilder getApplicationCloudModelBuilder(DelegateExecution context) {
        return StepsUtil.getApplicationCloudModelBuilder(context, getStepLogger());
    }

    private ConfigurationEntriesCloudModelBuilder getConfigurationEntriesCloudModelBuilder(DelegateExecution context) {
        String orgName = StepsUtil.getOrg(context);
        String spaceName = StepsUtil.getSpace(context);
        String spaceId = StepsUtil.getSpaceId(context);
        return new ConfigurationEntriesCloudModelBuilder(orgName, spaceName, spaceId);
    }

}
