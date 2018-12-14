package com.sap.cloud.lm.sl.cf.process.steps;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.SetUtils;
import org.cloudfoundry.client.lib.domain.ServiceKey;
import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.cf.util.CloudModelBuilderContentCalculator;
import com.sap.cloud.lm.sl.cf.core.cf.util.ModulesCloudModelBuilderContentCalculator;
import com.sap.cloud.lm.sl.cf.core.cf.util.ResourcesCloudModelBuilderContentCalculator;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ApplicationsCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ServiceKeysCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ServicesCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.helpers.ModuleToDeployHelper;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.PropertiesAccessor;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.model.ModuleToDeploy;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2.Module;
import com.sap.cloud.lm.sl.mta.model.v2.Resource;

public class BuildCloudDeployModelStep extends SyncFlowableStep {

    @Inject
    private ModuleToDeployHelper moduleToDeployHelper;

    protected SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        try {
            getStepLogger().debug(Messages.BUILDING_CLOUD_MODEL);
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

            // Build a map of service keys and save them in the context:
            Map<String, List<ServiceKey>> serviceKeys = getServiceKeysCloudModelBuilder(execution.getContext()).build();
            getStepLogger().debug(Messages.SERVICE_KEYS_TO_CREATE, secureSerializer.toJson(serviceKeys));

            StepsUtil.setServiceKeysToCreate(execution.getContext(), serviceKeys);

            // Build a list of applications for deployment and save them in the context:
            List<Module> modulesCalculatedForDeployment = calculateModulesForDeployment(execution, deploymentDescriptor, mtaArchiveModules,
                deployedModuleNames, mtaModules);

            List<ModuleToDeploy> modulesToDeploy = getModulesToDeploy(modulesCalculatedForDeployment);
            validateNoUnresolvedModulesExist(deployedModuleNames, mtaModules, modulesToDeploy);
            
            getStepLogger().debug(Messages.MODULES_TO_DEPLOY, secureSerializer.toJson(modulesToDeploy));
            StepsUtil.setAllModulesToDeploy(execution.getContext(), modulesToDeploy);

            ApplicationsCloudModelBuilder applicationsCloudModelBuilder = getApplicationsCloudModelBuilder(execution.getContext());
            List<CloudApplicationExtended> apps = applicationsCloudModelBuilder.build(modulesCalculatedForDeployment, moduleToDeployHelper);

            getStepLogger().debug(Messages.APPS_TO_DEPLOY, secureSerializer.toJson(apps));
            StepsUtil.setAppsToDeploy(execution.getContext(), apps);
            StepsUtil.setDeploymentMode(execution.getContext(), applicationsCloudModelBuilder.getDeploymentMode());
            StepsUtil.setServiceKeysCredentialsToInject(execution.getContext(), new HashMap<>());
            StepsUtil.setUseIdleUris(execution.getContext(), false);

            // Build a list of custom domains and save them in the context:
            List<String> customDomainsFromApps = StepsUtil.getDomainsFromApps(execution.getContext(), apps);
            StepsUtil.setCustomDomains(execution.getContext(), customDomainsFromApps);
            getStepLogger().debug(Messages.CUSTOM_DOMAINS, customDomainsFromApps);

            List<Resource> resourcesCalculatedForDeployment = calculateResourcesForDeployment(execution, deploymentDescriptor);

            List<CloudServiceExtended> allServices = getServicesCloudModelBuilder(execution.getContext())
                .build(resourcesCalculatedForDeployment);

            // Build a list of services for binding and save them in the context:
            StepsUtil.setServicesToBind(execution.getContext(), allServices);

            // Build a list of services for creation and save them in the context:
            List<CloudServiceExtended> servicesToCreate = allServices.stream()
                .filter(CloudServiceExtended::isManaged)
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

    private List<ModuleToDeploy> getModulesToDeploy(List<Module> modules) {
        List<ModuleToDeploy> modulesToDeploy = new ArrayList<ModuleToDeploy>();
        for (Module module : modules) {
            ModuleToDeploy moduleToDeploy = new ModuleToDeploy(module.getName(), module.getType());
            if (module instanceof com.sap.cloud.lm.sl.mta.model.v3.Module) {
                List<String> deployedAfter = emptyIfNull(((com.sap.cloud.lm.sl.mta.model.v3.Module) module).getDeployedAfter());
                moduleToDeploy.setDeployedAfter(new HashSet<>(deployedAfter));
            }
            modulesToDeploy.add(moduleToDeploy);
        }
        return modulesToDeploy;
    }

    private List<Resource> calculateResourcesForDeployment(ExecutionWrapper execution, DeploymentDescriptor deploymentDescriptor) {
        CloudModelBuilderContentCalculator<Resource> resourcesCloudModelBuilderContentCalculator = getResourcesCloudModelBuilderContentCalculator(
            execution.getContext());

        List<Resource> resourcesCalculatedForDeployment = calculateResourcesForDeployment(deploymentDescriptor,
            resourcesCloudModelBuilderContentCalculator);
        return resourcesCalculatedForDeployment;
    }

    private List<Module> calculateModulesForDeployment(ExecutionWrapper execution, DeploymentDescriptor deploymentDescriptor,
        Set<String> mtaArchiveModules, Set<String> deployedModuleNames, Set<String> mtaModules) {
        CloudModelBuilderContentCalculator<Module> modulesCloudModelBuilderContentCalculator = getModulesContentCalculator(execution,
            mtaArchiveModules, deployedModuleNames, mtaModules);
        return modulesCloudModelBuilderContentCalculator
            .calculateContentForBuilding(getModulesForDeployment(execution.getContext(), deploymentDescriptor));
    }

    private List<Resource> calculateResourcesForDeployment(DeploymentDescriptor deploymentDescriptor,
        CloudModelBuilderContentCalculator<Resource> resourcesCloudModelBuilderContentCalculator) {
        return resourcesCloudModelBuilderContentCalculator.calculateContentForBuilding(deploymentDescriptor.getResources2());
    }

    private CloudModelBuilderContentCalculator<Resource> getResourcesCloudModelBuilderContentCalculator(DelegateExecution context) {
        List<String> resourcesSpecifiedForDeployment = StepsUtil.getResourcesForDeployment(context);
        PropertiesAccessor propertiesAccessor = StepsUtil.getHandlerFactory(context)
            .getPropertiesAccessor();
        return new ResourcesCloudModelBuilderContentCalculator(resourcesSpecifiedForDeployment, propertiesAccessor, getStepLogger());
    }

    private void validateNoUnresolvedModulesExist(Set<String> deployedModuleNames, Set<String> mtaModules,
        List<ModuleToDeploy> modulesToDeploy) {
        Set<String> unresolvedModules = getUnresolvedModules(modulesToDeploy, deployedModuleNames, mtaModules);
        if (!unresolvedModules.isEmpty()) {
            throw new ContentException(com.sap.cloud.lm.sl.cf.core.message.Messages.UNRESOLVED_MTA_MODULES, unresolvedModules);
        }
    }

    private Set<String> getUnresolvedModules(List<ModuleToDeploy> modulesToDeploy, Set<String> deployedModules, Set<String> allMtaModules) {
        Set<String> resolvedModules = modulesToDeploy.stream()
            .map(ModuleToDeploy::getName)
            .collect(Collectors.toSet());
        return SetUtils.difference(allMtaModules, SetUtils.union(resolvedModules, deployedModules))
            .toSet();
    }

    private ModulesCloudModelBuilderContentCalculator getModulesContentCalculator(ExecutionWrapper execution, Set<String> mtaArchiveModules,
        Set<String> deployedModuleNames, Set<String> allMtaModules) {
        PropertiesAccessor propertiesAccessor = StepsUtil.getHandlerFactory(execution.getContext())
            .getPropertiesAccessor();
        return new ModulesCloudModelBuilderContentCalculator(mtaArchiveModules, deployedModuleNames, allMtaModules,
            StepsUtil.getModulesForDeployment(execution.getContext()), propertiesAccessor, getStepLogger(), moduleToDeployHelper);
    }

    private List<? extends Module> getModulesForDeployment(DelegateExecution context, DeploymentDescriptor deploymentDescriptor) {
        HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context);
        DescriptorHandler handler = handlerFactory.getDescriptorHandler();
        return handler.getModulesForDeployment(deploymentDescriptor, SupportedParameters.ENABLE_PARALLEL_DEPLOYMENTS,
            SupportedParameters.DEPENDENCY_TYPE, com.sap.cloud.lm.sl.cf.core.Constants.DEPENDENCY_TYPE_HARD);
    }

    protected ServiceKeysCloudModelBuilder getServiceKeysCloudModelBuilder(DelegateExecution context) {
        return StepsUtil.getServiceKeysCloudModelBuilder(context);
    }

    protected ApplicationsCloudModelBuilder getApplicationsCloudModelBuilder(DelegateExecution context) {
        return StepsUtil.getApplicationsCloudModelBuilder(context);
    }

    protected ServicesCloudModelBuilder getServicesCloudModelBuilder(DelegateExecution context) {
        return StepsUtil.getServicesCloudModelBuilder(context);
    }

}
