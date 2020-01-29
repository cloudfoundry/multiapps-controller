package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudServiceKey;
import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.cf.util.CloudModelBuilderContentCalculator;
import com.sap.cloud.lm.sl.cf.core.cf.util.DeployedAfterModulesContentValidator;
import com.sap.cloud.lm.sl.cf.core.cf.util.ModulesCloudModelBuilderContentCalculator;
import com.sap.cloud.lm.sl.cf.core.cf.util.ModulesContentValidator;
import com.sap.cloud.lm.sl.cf.core.cf.util.ResourcesCloudModelBuilderContentCalculator;
import com.sap.cloud.lm.sl.cf.core.cf.util.UnresolvedModulesContentValidator;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ApplicationCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ServiceKeysCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ServicesCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.helpers.ModuleToDeployHelper;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Module;
import com.sap.cloud.lm.sl.mta.model.RequiredDependency;
import com.sap.cloud.lm.sl.mta.model.Resource;

public class BuildCloudDeployModelStep extends SyncFlowableStep {

    @Inject
    private ModuleToDeployHelper moduleToDeployHelper;

    protected final SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        getStepLogger().debug(Messages.BUILDING_CLOUD_MODEL);
        DeploymentDescriptor deploymentDescriptor = StepsUtil.getCompleteDeploymentDescriptor(execution.getContext());

        // Get module sets:
        DeployedMta deployedMta = StepsUtil.getDeployedMta(execution.getContext());
        List<DeployedMtaApplication> deployedApplications = (deployedMta != null) ? deployedMta.getApplications() : Collections.emptyList();
        Set<String> mtaArchiveModules = StepsUtil.getMtaArchiveModules(execution.getContext());
        getStepLogger().debug(Messages.MTA_ARCHIVE_MODULES, mtaArchiveModules);
        Set<String> deployedModuleNames = CloudModelBuilderUtil.getDeployedModuleNames(deployedApplications);
        getStepLogger().debug(Messages.DEPLOYED_MODULES, deployedModuleNames);
        Set<String> mtaModules = StepsUtil.getMtaModules(execution.getContext());
        getStepLogger().debug(Messages.MTA_MODULES, mtaModules);

        StepsUtil.setNewMtaVersion(execution.getContext(), deploymentDescriptor.getVersion());

        // Build a map of service keys and save them in the context:
        Map<String, List<CloudServiceKey>> serviceKeys = getServiceKeysCloudModelBuilder(execution.getContext()).build();
        getStepLogger().debug(Messages.SERVICE_KEYS_TO_CREATE, secureSerializer.toJson(serviceKeys));

        StepsUtil.setServiceKeysToCreate(execution.getContext(), serviceKeys);

        // Build a list of applications for deployment and save them in the context:
        List<Module> modulesCalculatedForDeployment = calculateModulesForDeployment(execution, deploymentDescriptor, mtaArchiveModules,
                                                                                    deployedModuleNames, mtaModules);

        getStepLogger().debug(Messages.MODULES_TO_DEPLOY, secureSerializer.toJson(modulesCalculatedForDeployment));
        StepsUtil.setAllModulesToDeploy(execution.getContext(), modulesCalculatedForDeployment);
        StepsUtil.setModulesToDeploy(execution.getContext(), modulesCalculatedForDeployment);

        ApplicationCloudModelBuilder applicationCloudModelBuilder = getApplicationCloudModelBuilder(execution.getContext());

        StepsUtil.setAppsToDeploy(execution.getContext(), getAppNames(modulesCalculatedForDeployment));

        StepsUtil.setDeploymentMode(execution.getContext(), applicationCloudModelBuilder.getDeploymentMode());
        StepsUtil.setServiceKeysCredentialsToInject(execution.getContext(), new HashMap<>());
        StepsUtil.setUseIdleUris(execution.getContext(), false);

        // Build a list of custom domains and save them in the context:
        List<String> customDomainsFromApps = StepsUtil.getDomainsFromApps(execution.getContext(), deploymentDescriptor,
                                                                          applicationCloudModelBuilder, modulesCalculatedForDeployment,
                                                                          moduleToDeployHelper);
        StepsUtil.setCustomDomains(execution.getContext(), customDomainsFromApps);
        getStepLogger().debug(Messages.CUSTOM_DOMAINS, customDomainsFromApps);

        ServicesCloudModelBuilder servicesCloudModelBuilder = getServicesCloudModelBuilder(execution.getContext());

        List<Resource> resourcesUsedForBindings = calculateResourcesUsedForBindings(deploymentDescriptor, modulesCalculatedForDeployment);
        List<CloudServiceExtended> servicesForBindings = servicesCloudModelBuilder.build(resourcesUsedForBindings);

        // Build a list of services for binding and save them in the context:
        StepsUtil.setServicesToBind(execution.getContext(), servicesForBindings);

        List<Resource> resourcesForDeployment = calculateResourcesForDeployment(execution, deploymentDescriptor);
        List<CloudServiceExtended> servicesCalculatedForDeployment = servicesCloudModelBuilder.build(resourcesForDeployment);

        // Build a list of services for creation and save them in the context:
        List<CloudServiceExtended> servicesToCreate = servicesCalculatedForDeployment.stream()
                                                                                     .filter(CloudServiceExtended::isManaged)
                                                                                     .collect(Collectors.toList());
        getStepLogger().debug(Messages.SERVICES_TO_CREATE, secureSerializer.toJson(servicesToCreate));
        StepsUtil.setServicesToCreate(execution.getContext(), servicesToCreate);

        // Needed by CreateOrUpdateServicesStep, as it is used as an iteration variable:
        execution.getContext()
                 .setVariable(Constants.VAR_SERVICES_TO_CREATE_COUNT, servicesToCreate.size());

        getStepLogger().debug(Messages.CLOUD_MODEL_BUILT);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(DelegateExecution context) {
        return Messages.ERROR_BUILDING_CLOUD_MODEL;
    }

    private List<String> getAppNames(List<Module> modulesCalculatedForDeployment) {
        return modulesCalculatedForDeployment.stream()
                                             .filter(module -> moduleToDeployHelper.isApplication(module))
                                             .map(NameUtil::getApplicationName)
                                             .collect(Collectors.toList());
    }

    private List<Resource> calculateResourcesForDeployment(ExecutionWrapper execution, DeploymentDescriptor deploymentDescriptor) {
        CloudModelBuilderContentCalculator<Resource> resourcesCloudModelBuilderContentCalculator = getResourcesCloudModelBuilderContentCalculator(execution.getContext());

        return calculateResourcesForDeployment(deploymentDescriptor, resourcesCloudModelBuilderContentCalculator);
    }

    private List<Resource> calculateResourcesUsedForBindings(DeploymentDescriptor deploymentDescriptor,
                                                             List<Module> modulesCalculatedForDeployment) {
        List<String> resourcesRequiredByModules = calculateResourceNamesRequiredByModules(modulesCalculatedForDeployment,
                                                                                          deploymentDescriptor.getResources());
        CloudModelBuilderContentCalculator<Resource> resourcesCloudModelBuilderContentCalculator = new ResourcesCloudModelBuilderContentCalculator(resourcesRequiredByModules,
                                                                                                                                                   getStepLogger());
        return resourcesCloudModelBuilderContentCalculator.calculateContentForBuilding(deploymentDescriptor.getResources());
    }

    private List<String> calculateResourceNamesRequiredByModules(List<Module> modulesCalculatedForDeployment,
                                                                 List<Resource> resourcesInDeploymentDescriptor) {
        Set<String> requiredDependencies = getRequireDependencyNamesFromModules(modulesCalculatedForDeployment);
        return getDependencyNamesToResources(resourcesInDeploymentDescriptor, requiredDependencies);
    }

    private Set<String> getRequireDependencyNamesFromModules(List<Module> modules) {
        return modules.stream()
                      .flatMap(module -> module.getRequiredDependencies()
                                               .stream()
                                               .map(RequiredDependency::getName))
                      .collect(Collectors.toSet());
    }

    private List<String> getDependencyNamesToResources(List<Resource> resources, Set<String> requiredDependencies) {
        return resources.stream()
                        .map(Resource::getName)
                        .filter(requiredDependencies::contains)
                        .collect(Collectors.toList());
    }

    private List<Module> calculateModulesForDeployment(ExecutionWrapper execution, DeploymentDescriptor deploymentDescriptor,
                                                       Set<String> mtaArchiveModules, Set<String> deployedModuleNames,
                                                       Set<String> mtaModules) {
        CloudModelBuilderContentCalculator<Module> modulesCloudModelBuilderContentCalculator = getModulesContentCalculator(execution,
                                                                                                                           mtaArchiveModules,
                                                                                                                           deployedModuleNames,
                                                                                                                           mtaModules);
        return modulesCloudModelBuilderContentCalculator.calculateContentForBuilding(getModulesForDeployment(execution.getContext(),
                                                                                                             deploymentDescriptor));
    }

    private List<Resource>
            calculateResourcesForDeployment(DeploymentDescriptor deploymentDescriptor,
                                            CloudModelBuilderContentCalculator<Resource> resourcesCloudModelBuilderContentCalculator) {
        return resourcesCloudModelBuilderContentCalculator.calculateContentForBuilding(deploymentDescriptor.getResources());
    }

    private CloudModelBuilderContentCalculator<Resource> getResourcesCloudModelBuilderContentCalculator(DelegateExecution context) {
        List<String> resourcesSpecifiedForDeployment = StepsUtil.getResourcesForDeployment(context);
        return new ResourcesCloudModelBuilderContentCalculator(resourcesSpecifiedForDeployment, getStepLogger());
    }

    protected ModulesCloudModelBuilderContentCalculator
              getModulesContentCalculator(ExecutionWrapper execution, Set<String> mtaArchiveModules, Set<String> deployedModuleNames,
                                          Set<String> allMtaModules) {
        return new ModulesCloudModelBuilderContentCalculator(mtaArchiveModules,
                                                             deployedModuleNames,
                                                             StepsUtil.getModulesForDeployment(execution.getContext()),
                                                             getStepLogger(),
                                                             moduleToDeployHelper,
                                                             getModuleContentValidators(execution.getControllerClient(), allMtaModules,
                                                                                        deployedModuleNames));
    }

    private List<ModulesContentValidator> getModuleContentValidators(CloudControllerClient cloudControllerClient, Set<String> allMtaModules,
                                                                     Set<String> deployedModuleNames) {
        return Arrays.asList(new UnresolvedModulesContentValidator(allMtaModules, deployedModuleNames),
                             new DeployedAfterModulesContentValidator(cloudControllerClient));
    }

    private List<? extends Module> getModulesForDeployment(DelegateExecution context, DeploymentDescriptor deploymentDescriptor) {
        HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context);
        DescriptorHandler handler = handlerFactory.getDescriptorHandler();
        return handler.getModulesForDeployment(deploymentDescriptor, SupportedParameters.ENABLE_PARALLEL_DEPLOYMENTS,
                                               SupportedParameters.DEPENDENCY_TYPE,
                                               com.sap.cloud.lm.sl.cf.core.Constants.DEPENDENCY_TYPE_HARD);
    }

    protected ServiceKeysCloudModelBuilder getServiceKeysCloudModelBuilder(DelegateExecution context) {
        return StepsUtil.getServiceKeysCloudModelBuilder(context);
    }

    protected ApplicationCloudModelBuilder getApplicationCloudModelBuilder(DelegateExecution context) {
        return StepsUtil.getApplicationCloudModelBuilder(context, getStepLogger());
    }

    protected ServicesCloudModelBuilder getServicesCloudModelBuilder(DelegateExecution context) {
        return StepsUtil.getServicesCloudModelBuilder(context);
    }

}
