package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ApplicationCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ConfigurationEntriesCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.helpers.ModuleToDeployHelper;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Module;

public class BuildApplicationDeployModelStep extends SyncFlowableStep {

    @Inject
    private ModuleToDeployHelper moduleToDeployHelper;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        Module module = StepsUtil.getModuleToDeploy(execution.getContext());
        getStepLogger().debug(Messages.BUILDING_CLOUD_APP_MODEL, module.getName());

        Module applicationModule = StepsUtil.findModuleInDeploymentDescriptor(execution.getContext(), module.getName());
        StepsUtil.setModuleToDeploy(execution.getContext(), applicationModule);
        CloudApplicationExtended modifiedApp = getApplicationCloudModelBuilder(execution.getContext()).build(applicationModule,
                                                                                                             moduleToDeployHelper);
        modifiedApp = ImmutableCloudApplicationExtended.builder()
                                                       .from(modifiedApp)
                                                       .env(getApplicationEnv(execution.getContext(), modifiedApp))
                                                       .uris(getApplicationUris(execution.getContext(), modifiedApp))
                                                       .build();
        SecureSerializationFacade secureSerializationFacade = new SecureSerializationFacade();
        String appJson = secureSerializationFacade.toJson(modifiedApp);
        getStepLogger().debug(Messages.APP_WITH_UPDATED_ENVIRONMENT, appJson);
        StepsUtil.setApp(execution.getContext(), modifiedApp);

        buildConfigurationEntries(execution.getContext(), modifiedApp);
        StepsUtil.setTasksToExecute(execution.getContext(), modifiedApp.getTasks());

        getStepLogger().debug(Messages.CLOUD_APP_MODEL_BUILT);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(DelegateExecution context) {
        return Messages.ERROR_BUILDING_CLOUD_APP_MODEL;
    }

    protected Map<String, String> getApplicationEnv(DelegateExecution context, CloudApplicationExtended app) {
        return app.getEnv();
    }

    private List<String> getApplicationUris(DelegateExecution context, CloudApplicationExtended modifiedApp) {
        if (StepsUtil.getUseIdleUris(context)) {
            return modifiedApp.getIdleUris();
        }
        return modifiedApp.getUris();
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
