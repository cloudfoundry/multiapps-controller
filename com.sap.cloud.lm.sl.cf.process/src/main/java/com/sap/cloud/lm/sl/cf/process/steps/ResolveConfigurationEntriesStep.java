package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.ConfigurationEntriesCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;

@Component("resolveConfigurationEntriesStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ResolveConfigurationEntriesStep extends BuildCloudDeployModelStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws SLException {
        try {
            getStepLogger().info(Messages.RESOLVING_CONFIGURATION_ENTRIES);
            DeploymentDescriptor deploymentDescriptor = StepsUtil.getDeploymentDescriptor(execution.getContext());

            // Get module sets:
            DeployedMta deployedMta = StepsUtil.getDeployedMta(execution.getContext());
            List<DeployedMtaModule> deployedModules = (deployedMta != null) ? deployedMta.getModules() : Collections.emptyList();
            Set<String> mtaArchiveModules = StepsUtil.getMtaArchiveModules(execution.getContext());
            Set<String> deployedModuleNames = CloudModelBuilderUtil.getDeployedModuleNames(deployedModules);
            Set<String> mtaModules = StepsUtil.getMtaModules(execution.getContext());

            // Build a list of applications for deployment and save them in the context:
            List<CloudApplicationExtended> apps = getApplicationsCloudModelBuilder(execution.getContext()).build(mtaArchiveModules,
                mtaModules, deployedModuleNames);

            // Build public provided dependencies list and save them in the context:
            ConfigurationEntriesCloudModelBuilder configurationEntriesCloudModelBuilder = getConfigurationEntriesCloudModelBuilder(
                execution.getContext());
            Map<String, List<ConfigurationEntry>> configurationEntries = configurationEntriesCloudModelBuilder.build(deploymentDescriptor);
            Map<String, List<ConfigurationEntry>> updatedModuleNames = updateModuleNames(configurationEntries, apps);
            StepsUtil.setConfigurationEntriesToPublish(execution.getContext(), updatedModuleNames);
            StepsUtil.setSkipUpdateConfigurationEntries(execution.getContext(), false);

            getStepLogger().debug(Messages.CONFIGURATION_ENTRIES_TO_PUBLISH, secureSerializer.toJson(updatedModuleNames));

            return StepPhase.DONE;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_RESOLVING_CONFIGURATION_ENTRIES);
            throw e;
        }
    }

}
