package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ConfigurationEntriesCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.helpers.ModuleToDeployHelper;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Module;

public class BuildApplicationDeployModelStep extends SyncFlowableStep {

    @Inject
    private ModuleToDeployHelper moduleToDeployHelper;

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        Module module = context.getVariable(Variables.MODULE_TO_DEPLOY);
        getStepLogger().debug(Messages.BUILDING_CLOUD_APP_MODEL, module.getName());

        Module applicationModule = findModuleInDeploymentDescriptor(context, module.getName());
        context.setVariable(Variables.MODULE_TO_DEPLOY, applicationModule);
        CloudApplicationExtended modifiedApp = StepsUtil.getApplicationCloudModelBuilder(context)
                                                        .build(applicationModule, moduleToDeployHelper);
        modifiedApp = ImmutableCloudApplicationExtended.builder()
                                                       .from(modifiedApp)
                                                       .env(getApplicationEnv(context, modifiedApp))
                                                       .uris(getApplicationUris(context, modifiedApp))
                                                       .build();
        SecureSerializationFacade secureSerializationFacade = new SecureSerializationFacade();
        String appJson = secureSerializationFacade.toJson(modifiedApp);
        getStepLogger().debug(Messages.APP_WITH_UPDATED_ENVIRONMENT, appJson);
        context.setVariable(Variables.APP_TO_PROCESS, modifiedApp);

        buildConfigurationEntries(context, modifiedApp);
        context.setVariable(Variables.TASKS_TO_EXECUTE, modifiedApp.getTasks());

        getStepLogger().debug(Messages.CLOUD_APP_MODEL_BUILT);
        return StepPhase.DONE;
    }

    private Module findModuleInDeploymentDescriptor(ProcessContext context, String module) {
        HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context.getExecution());
        DeploymentDescriptor deploymentDescriptor = context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR);
        return handlerFactory.getDescriptorHandler()
                             .findModule(deploymentDescriptor, module);
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_BUILDING_CLOUD_APP_MODEL;
    }

    protected Map<String, String> getApplicationEnv(ProcessContext context, CloudApplicationExtended app) {
        return app.getEnv();
    }

    private List<String> getApplicationUris(ProcessContext context, CloudApplicationExtended modifiedApp) {
        if (context.getVariable(Variables.USE_IDLE_URIS)) {
            return modifiedApp.getIdleUris();
        }
        return modifiedApp.getUris();
    }

    private void buildConfigurationEntries(ProcessContext context, CloudApplicationExtended app) {
        if (context.getVariable(Variables.SKIP_UPDATE_CONFIGURATION_ENTRIES)) {
            context.setVariable(Variables.CONFIGURATION_ENTRIES_TO_PUBLISH, Collections.emptyList());
            return;
        }
        DeploymentDescriptor deploymentDescriptor = context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR);

        ConfigurationEntriesCloudModelBuilder configurationEntriesCloudModelBuilder = getConfigurationEntriesCloudModelBuilder(context);
        Map<String, List<ConfigurationEntry>> allConfigurationEntries = configurationEntriesCloudModelBuilder.build(deploymentDescriptor);
        List<ConfigurationEntry> updatedModuleNames = allConfigurationEntries.getOrDefault(app.getModuleName(), Collections.emptyList());
        context.setVariable(Variables.CONFIGURATION_ENTRIES_TO_PUBLISH, updatedModuleNames);
        context.setVariable(Variables.SKIP_UPDATE_CONFIGURATION_ENTRIES, false);

        getStepLogger().debug(Messages.CONFIGURATION_ENTRIES_TO_PUBLISH, JsonUtil.toJson(updatedModuleNames, true));
    }

    private ConfigurationEntriesCloudModelBuilder getConfigurationEntriesCloudModelBuilder(ProcessContext context) {
        String orgName = context.getVariable(Variables.ORG);
        String spaceName = context.getVariable(Variables.SPACE);
        String spaceId = context.getVariable(Variables.SPACE_ID);
        return new ConfigurationEntriesCloudModelBuilder(orgName, spaceName, spaceId);
    }

}
