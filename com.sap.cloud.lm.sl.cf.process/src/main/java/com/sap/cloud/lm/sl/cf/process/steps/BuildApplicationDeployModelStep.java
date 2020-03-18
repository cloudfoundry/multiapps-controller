package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.flowable.engine.delegate.DelegateExecution;

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
    protected StepPhase executeStep(ExecutionWrapper execution) {
        Module module = StepsUtil.getModuleToDeploy(execution.getContext());
        getStepLogger().debug(Messages.BUILDING_CLOUD_APP_MODEL, module.getName());

        Module applicationModule = findModuleInDeploymentDescriptor(execution, module.getName());
        StepsUtil.setModuleToDeploy(execution.getContext(), applicationModule);
        CloudApplicationExtended modifiedApp = StepsUtil.getApplicationCloudModelBuilder(execution)
                                                        .build(applicationModule, moduleToDeployHelper);
        modifiedApp = ImmutableCloudApplicationExtended.builder()
                                                       .from(modifiedApp)
                                                       .env(getApplicationEnv(execution.getContext(), modifiedApp))
                                                       .uris(getApplicationUris(execution.getContext(), modifiedApp))
                                                       .build();
        SecureSerializationFacade secureSerializationFacade = new SecureSerializationFacade();
        String appJson = secureSerializationFacade.toJson(modifiedApp);
        getStepLogger().debug(Messages.APP_WITH_UPDATED_ENVIRONMENT, appJson);
        execution.setVariable(Variables.APP_TO_PROCESS, modifiedApp);

        buildConfigurationEntries(execution, modifiedApp);
        StepsUtil.setTasksToExecute(execution.getContext(), modifiedApp.getTasks());

        getStepLogger().debug(Messages.CLOUD_APP_MODEL_BUILT);
        return StepPhase.DONE;
    }

    private Module findModuleInDeploymentDescriptor(ExecutionWrapper execution, String module) {
        HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(execution.getContext());
        DeploymentDescriptor deploymentDescriptor = execution.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR);
        return handlerFactory.getDescriptorHandler()
                             .findModule(deploymentDescriptor, module);
    }

    @Override
    protected String getStepErrorMessage(ExecutionWrapper execution) {
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

    private void buildConfigurationEntries(ExecutionWrapper execution, CloudApplicationExtended app) {
        if (StepsUtil.getSkipUpdateConfigurationEntries(execution.getContext())) {
            StepsUtil.setConfigurationEntriesToPublish(execution.getContext(), Collections.emptyList());
            return;
        }
        DeploymentDescriptor deploymentDescriptor = execution.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR);

        ConfigurationEntriesCloudModelBuilder configurationEntriesCloudModelBuilder = getConfigurationEntriesCloudModelBuilder(execution.getContext());
        Map<String, List<ConfigurationEntry>> allConfigurationEntries = configurationEntriesCloudModelBuilder.build(deploymentDescriptor);
        List<ConfigurationEntry> updatedModuleNames = allConfigurationEntries.getOrDefault(app.getModuleName(), Collections.emptyList());
        StepsUtil.setConfigurationEntriesToPublish(execution.getContext(), updatedModuleNames);
        StepsUtil.setSkipUpdateConfigurationEntries(execution.getContext(), false);

        getStepLogger().debug(Messages.CONFIGURATION_ENTRIES_TO_PUBLISH, JsonUtil.toJson(updatedModuleNames, true));
    }

    private ConfigurationEntriesCloudModelBuilder getConfigurationEntriesCloudModelBuilder(DelegateExecution context) {
        String orgName = StepsUtil.getOrg(context);
        String spaceName = StepsUtil.getSpace(context);
        String spaceId = StepsUtil.getSpaceId(context);
        return new ConfigurationEntriesCloudModelBuilder(orgName, spaceName, spaceId);
    }

}
