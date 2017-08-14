package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ServiceKey;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.ApplicationsCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.ConfigurationEntriesCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.DomainsCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.ServiceKeysCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.ServicesCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("buildCloudDeployModelStep")
public class BuildCloudDeployModelStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(BuildCloudDeployModelStep.class);

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("buildDeployModelTask").displayName("Build Deploy Model").description(
            "Build Deploy Model").build();
    }

    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        logActivitiTask(context, LOGGER);
        try {
            info(context, Messages.BUILDING_CLOUD_MODEL, LOGGER);
            DeploymentDescriptor deploymentDescriptor = StepsUtil.getDeploymentDescriptor(context);

            // Get module sets:
            DeployedMta deployedMta = StepsUtil.getDeployedMta(context);
            List<DeployedMtaModule> deployedModules = (deployedMta != null) ? deployedMta.getModules() : Collections.emptyList();
            Set<String> mtaArchiveModules = StepsUtil.getMtaArchiveModules(context);
            debug(context, format(Messages.MTA_ARCHIVE_MODULES, mtaArchiveModules), LOGGER);
            Set<String> deployedModuleNames = CloudModelBuilderUtil.getDeployedModuleNames(deployedModules);
            debug(context, format(Messages.DEPLOYED_MODULES, deployedModuleNames), LOGGER);
            Set<String> mtaModules = StepsUtil.getMtaModules(context);
            debug(context, format(Messages.MTA_MODULES, mtaModules), LOGGER);

            StepsUtil.setNewMtaVersion(context, deploymentDescriptor.getVersion());

            // Build a list of custom domains and save them in the context:
            List<String> customDomains = getDomainsCloudModelBuilder(context).build();
            debug(context, format(Messages.CUSTOM_DOMAINS, customDomains), LOGGER);
            StepsUtil.setCustomDomains(context, customDomains);

            // Build a map of service keys and save them in the context:
            Map<String, List<ServiceKey>> serviceKeys = getServiceKeysCloudModelBuilder(context, deploymentDescriptor).build();
            debug(context, format(Messages.SERVICE_KEYS_TO_CREATE, secureSerializer.toJson(serviceKeys)), LOGGER);

            StepsUtil.setServiceKeysToCreate(context, serviceKeys);

            // Build a list of applications for deployment and save them in the context:
            List<CloudApplicationExtended> apps = getApplicationsCloudModelBuilder(context).build(mtaArchiveModules, mtaModules,
                deployedModuleNames);
            debug(context, format(Messages.APPS_TO_DEPLOY, secureSerializer.toJson(apps)), LOGGER);
            StepsUtil.setAppsToDeploy(context, apps);

            // Build public provided dependencies list and save them in the context:
            ConfigurationEntriesCloudModelBuilder configurationEntriesCloudModelBuilder = getConfigurationEntriesCloudModelBuilder(context);
            Map<String, List<ConfigurationEntry>> configurationEntries = configurationEntriesCloudModelBuilder.build(deploymentDescriptor);
            Map<String, List<ConfigurationEntry>> updatedModuleNames = updateModuleNames(configurationEntries, apps);
            StepsUtil.setConfigurationEntriesToPublish(context, updatedModuleNames);

            // Build a list of services for creation and save them in the context:

            List<CloudServiceExtended> services = getServicesCloudModelBuilder(context).build(mtaArchiveModules);
            debug(context, format(Messages.SERVICES_TO_CREATE, secureSerializer.toJson(services)), LOGGER);

            StepsUtil.setServicesToCreate(context, services);
            // Needed by CreateOrUpdateServicesStep, as it is used as an iteration variable:
            context.setVariable(Constants.VAR_SERVICES_TO_CREATE_COUNT, 0);

            debug(context, Messages.CLOUD_MODEL_BUILT, LOGGER);
            return ExecutionStatus.SUCCESS;
        } catch (SLException e) {
            error(context, Messages.ERROR_BUILDING_CLOUD_MODEL, e, LOGGER);
            throw e;
        }
    }

    protected DomainsCloudModelBuilder getDomainsCloudModelBuilder(DelegateExecution context) {
        return StepsUtil.getDomainsCloudModelBuilder(context);
    }

    protected ServiceKeysCloudModelBuilder getServiceKeysCloudModelBuilder(DelegateExecution context,
        DeploymentDescriptor deploymentDescriptor) {
        return new ServiceKeysCloudModelBuilder(deploymentDescriptor, StepsUtil.getHandlerFactory(context).getPropertiesAccessor());
    }

    protected ApplicationsCloudModelBuilder getApplicationsCloudModelBuilder(DelegateExecution context) {
        return StepsUtil.getApplicationsCloudModelBuilder(context);
    }

    protected ServicesCloudModelBuilder getServicesCloudModelBuilder(DelegateExecution context) {
        return StepsUtil.getServicesCloudModelBuilder(context);
    }

    private Map<String, List<ConfigurationEntry>> updateModuleNames(Map<String, List<ConfigurationEntry>> configurationEntries,
        List<CloudApplicationExtended> apps) {
        Map<String, List<ConfigurationEntry>> result = new HashMap<>();
        for (CloudApplicationExtended app : apps) {
            List<ConfigurationEntry> configurationEntriesForModule = configurationEntries.getOrDefault(app.getModuleName(),
                Collections.emptyList());
            result.put(app.getName(), configurationEntriesForModule);
        }
        return result;
    }

    protected ConfigurationEntriesCloudModelBuilder getConfigurationEntriesCloudModelBuilder(DelegateExecution context) {
        String orgName = StepsUtil.getOrg(context);
        String spaceName = StepsUtil.getSpace(context);
        return new ConfigurationEntriesCloudModelBuilder(orgName, spaceName);
    }

}
