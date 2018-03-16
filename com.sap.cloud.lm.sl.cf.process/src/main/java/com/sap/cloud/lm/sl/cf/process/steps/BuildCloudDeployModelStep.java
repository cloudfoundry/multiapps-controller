package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.domain.ServiceKey;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
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

public class BuildCloudDeployModelStep extends SyncActivitiStep {

    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws SLException {
        getStepLogger().logActivitiTask();
        try {
            getStepLogger().info(Messages.BUILDING_CLOUD_MODEL);
            DeploymentDescriptor deploymentDescriptor = StepsUtil.getDeploymentDescriptor(execution.getContext());

            // Get module sets:
            DeployedMta deployedMta = StepsUtil.getDeployedMta(execution.getContext());
            List<DeployedMtaModule> deployedModules = (deployedMta != null) ? deployedMta.getModules() : Collections.emptyList();
            Set<String> mtaArchiveModules = StepsUtil.getMtaArchiveModules(execution.getContext());
            getStepLogger().debug(Messages.MTA_ARCHIVE_MODULES, mtaArchiveModules);
            Set<String> deployedModuleNames = CloudModelBuilderUtil.getDeployedModuleNames(deployedModules);
            getStepLogger().debug(Messages.DEPLOYED_MODULES, deployedModuleNames);
            Set<String> mtaModules = StepsUtil.getMtaModules(execution.getContext());
            getStepLogger().debug(Messages.MTA_MODULES, mtaModules);

            StepsUtil.setNewMtaVersion(execution.getContext(), deploymentDescriptor.getVersion());

            // Build a list of custom domains and save them in the context:
            List<String> customDomains = getDomainsCloudModelBuilder(execution.getContext()).build();
            getStepLogger().debug(Messages.CUSTOM_DOMAINS, customDomains);
            StepsUtil.setCustomDomains(execution.getContext(), customDomains);

            // Build a map of service keys and save them in the context:
            Map<String, List<ServiceKey>> serviceKeys = getServiceKeysCloudModelBuilder(execution.getContext(), deploymentDescriptor)
                .build();
            getStepLogger().debug(Messages.SERVICE_KEYS_TO_CREATE, secureSerializer.toJson(serviceKeys));

            StepsUtil.setServiceKeysToCreate(execution.getContext(), serviceKeys);

            // Build a list of applications for deployment and save them in the context:
            List<CloudApplicationExtended> apps = getApplicationsCloudModelBuilder(execution.getContext()).build(mtaArchiveModules,
                mtaModules, deployedModuleNames);
            getStepLogger().debug(Messages.APPS_TO_DEPLOY, secureSerializer.toJson(apps));
            StepsUtil.setAppsToDeploy(execution.getContext(), apps);
            StepsUtil.setServiceKeysCredentialsToInject(execution.getContext(), new HashMap<>());
            StepsUtil.setUseIdleUris(execution.getContext(), false);

            // Build public provided dependencies list and save them in the context:
            ConfigurationEntriesCloudModelBuilder configurationEntriesCloudModelBuilder = getConfigurationEntriesCloudModelBuilder(
                execution.getContext());
            Map<String, List<ConfigurationEntry>> configurationEntries = configurationEntriesCloudModelBuilder.build(deploymentDescriptor);
            Map<String, List<ConfigurationEntry>> updatedModuleNames = updateModuleNames(configurationEntries, apps);
            StepsUtil.setConfigurationEntriesToPublish(execution.getContext(), updatedModuleNames);

            List<CloudServiceExtended> allServices = getServicesCloudModelBuilder(execution.getContext()).build(mtaArchiveModules);

            // Build a list of services for binding and save them in the context:
            StepsUtil.setServicesToBind(execution.getContext(), allServices);

            // Build a list of services for creation and save them in the context:
            List<CloudServiceExtended> servicesToCreate = allServices.stream()
                .filter(service -> service.isManaged())
                .collect(Collectors.toList());
            getStepLogger().debug(Messages.SERVICES_TO_CREATE, secureSerializer.toJson(servicesToCreate));
            StepsUtil.setServicesToCreate(execution.getContext(), servicesToCreate);

            // Needed by CreateOrUpdateServicesStep, as it is used as an iteration variable:
            execution.getContext()
                .setVariable(Constants.VAR_SERVICES_TO_CREATE_COUNT, 0);

            getStepLogger().debug(Messages.CLOUD_MODEL_BUILT);
            return StepPhase.DONE;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_BUILDING_CLOUD_MODEL);
            throw e;
        }
    }

    protected DomainsCloudModelBuilder getDomainsCloudModelBuilder(DelegateExecution context) {
        return StepsUtil.getDomainsCloudModelBuilder(context);
    }

    protected ServiceKeysCloudModelBuilder getServiceKeysCloudModelBuilder(DelegateExecution context,
        DeploymentDescriptor deploymentDescriptor) {
        return new ServiceKeysCloudModelBuilder(deploymentDescriptor, StepsUtil.getHandlerFactory(context)
            .getPropertiesAccessor());
    }

    protected ApplicationsCloudModelBuilder getApplicationsCloudModelBuilder(DelegateExecution context) {
        return StepsUtil.getApplicationsCloudModelBuilder(context, getStepLogger());
    }

    protected ServicesCloudModelBuilder getServicesCloudModelBuilder(DelegateExecution context) {
        return StepsUtil.getServicesCloudModelBuilder(context, getStepLogger());
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
        String spaceId = StepsUtil.getSpaceId(context);
        return new ConfigurationEntriesCloudModelBuilder(orgName, spaceName, spaceId);
    }

}
