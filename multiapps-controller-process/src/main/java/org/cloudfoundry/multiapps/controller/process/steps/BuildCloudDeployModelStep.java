package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.cf.CloudHandlerFactory;
import org.cloudfoundry.multiapps.controller.core.cf.util.CloudModelBuilderContentCalculator;
import org.cloudfoundry.multiapps.controller.core.cf.util.DeployedAfterModulesContentValidator;
import org.cloudfoundry.multiapps.controller.core.cf.util.ModulesCloudModelBuilderContentCalculator;
import org.cloudfoundry.multiapps.controller.core.cf.util.ModulesContentValidator;
import org.cloudfoundry.multiapps.controller.core.cf.util.ResourcesCloudModelBuilderContentCalculator;
import org.cloudfoundry.multiapps.controller.core.cf.util.UnresolvedModulesContentValidator;
import org.cloudfoundry.multiapps.controller.core.cf.v2.ApplicationCloudModelBuilder;
import org.cloudfoundry.multiapps.controller.core.cf.v2.ServiceKeysCloudModelBuilder;
import org.cloudfoundry.multiapps.controller.core.cf.v2.ServicesCloudModelBuilder;
import org.cloudfoundry.multiapps.controller.core.helpers.ModuleToDeployHelper;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerialization;
import org.cloudfoundry.multiapps.controller.core.util.CloudModelBuilderUtil;
import org.cloudfoundry.multiapps.controller.core.util.NameUtil;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.builders.v2.ParametersChainBuilder;
import org.cloudfoundry.multiapps.mta.handlers.HandlerFactory;
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorHandler;
import org.cloudfoundry.multiapps.mta.handlers.v3.BatchCalculator;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.RequiredDependency;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.cloudfoundry.multiapps.mta.util.PropertiesUtil;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;

@Named("buildCloudDeployModelStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class BuildCloudDeployModelStep extends SyncFlowableStep {

    @Inject
    private ModuleToDeployHelper moduleToDeployHelper;

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.BUILDING_CLOUD_MODEL);
        DeploymentDescriptor deploymentDescriptor = context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR);

        // Get module sets:
        DeployedMta deployedMta = context.getVariable(Variables.DEPLOYED_MTA);
        List<DeployedMtaApplication> deployedApplications = (deployedMta != null) ? deployedMta.getApplications() : Collections.emptyList();
        Set<String> mtaManifestModulesNames = context.getVariable(Variables.MTA_ARCHIVE_MODULES);
        getStepLogger().debug(Messages.MTA_ARCHIVE_MODULES, mtaManifestModulesNames);
        List<Module> mtaDescriptorModules = deploymentDescriptor.getModules();
        Set<String> deployedModuleNames = CloudModelBuilderUtil.getDeployedModuleNames(deployedApplications);
        getStepLogger().debug(Messages.DEPLOYED_MODULES, deployedModuleNames);
        Set<String> mtaModulesForDeployment = context.getVariable(Variables.MTA_MODULES);
        getStepLogger().debug(Messages.MTA_MODULES, mtaModulesForDeployment);

        // Build a map of service keys and save them in the context:
        Map<String, List<CloudServiceKey>> serviceKeys = getServiceKeysCloudModelBuilder(context).build();
        getStepLogger().debug(Messages.SERVICE_KEYS_TO_CREATE, SecureSerialization.toJson(serviceKeys));

        context.setVariable(Variables.SERVICE_KEYS_TO_CREATE, serviceKeys);

        // Build a list of applications for deployment and save them in the context:
        List<Module> modulesCalculatedForDeployment = calculateModulesForDeployment(context, deploymentDescriptor, mtaDescriptorModules,
                                                                                    mtaManifestModulesNames, deployedModuleNames,
                                                                                    mtaModulesForDeployment);
        List<String> moduleJsons = modulesCalculatedForDeployment.stream()
                                                                 .map(SecureSerialization::toJson)
                                                                 .collect(Collectors.toList());
        getStepLogger().debug(Messages.MODULES_TO_DEPLOY, moduleJsons.toString());
        context.setVariable(Variables.ALL_MODULES_TO_DEPLOY, modulesCalculatedForDeployment);
        context.setVariable(Variables.MODULES_TO_DEPLOY, modulesCalculatedForDeployment);

        ApplicationCloudModelBuilder applicationCloudModelBuilder = getApplicationCloudModelBuilder(context);

        context.setVariable(Variables.APPS_TO_DEPLOY, getAppNames(modulesCalculatedForDeployment));

        context.setVariable(Variables.DEPLOYMENT_MODE, applicationCloudModelBuilder.getDeploymentMode());
        context.setVariable(Variables.SERVICE_KEYS_CREDENTIALS_TO_INJECT, new HashMap<>());
        context.setVariable(Variables.USE_IDLE_URIS, false);

        // Build a list of custom domains and save them in the context:
        List<String> customDomainsFromApps = getDomainsFromApps(context, deploymentDescriptor, applicationCloudModelBuilder,
                                                                modulesCalculatedForDeployment, moduleToDeployHelper);
        context.setVariable(Variables.CUSTOM_DOMAINS, customDomainsFromApps);
        getStepLogger().debug(Messages.CUSTOM_DOMAINS, customDomainsFromApps);

        ServicesCloudModelBuilder servicesCloudModelBuilder = getServicesCloudModelBuilder(context);

        List<Resource> resourcesUsedForBindings = calculateResourcesUsedForBindings(deploymentDescriptor, modulesCalculatedForDeployment);
        List<CloudServiceInstanceExtended> servicesForBindings = servicesCloudModelBuilder.build(resourcesUsedForBindings);

        // Build a list of services for binding and save them in the context:
        context.setVariable(Variables.SERVICES_TO_BIND, servicesForBindings);

        List<Resource> resourcesForDeployment = calculateResourcesForDeployment(context, deploymentDescriptor);
        // logg batching of resources
        getStepLogger().debug(Messages.CALCULATING_RESOURCE_BATCHES);
        // Organise resources into batches of resources
        List<List<Resource>> batchesToProcess = calculateBatchesToProcess(deploymentDescriptor, resourcesForDeployment);
        // set context variable batches_to_process
        context.setVariable(Variables.BATCHES_TO_PROCESS, batchesToProcess);
        // log end of the batching
        getStepLogger().debug(Messages.CALCULATING_RESOURCE_BATCHES_COMPLETE);

        getStepLogger().debug(Messages.CLOUD_MODEL_BUILT);
        return StepPhase.DONE;
    }

    private List<List<Resource>> calculateBatchesToProcess(DeploymentDescriptor deploymentDescriptor, List<Resource> resources) {
        if (!resources.isEmpty()) {
            BatchCalculator batchCalculator = new BatchCalculator(deploymentDescriptor);
            return new ArrayList<>(batchCalculator.groupByBatches(resources)
                                                  .values());
        }
        return Collections.emptyList();
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_BUILDING_CLOUD_MODEL;
    }

    private List<String> getAppNames(List<Module> modulesCalculatedForDeployment) {
        return modulesCalculatedForDeployment.stream()
                                             .filter(moduleToDeployHelper::isApplication)
                                             .map(NameUtil::getApplicationName)
                                             .collect(Collectors.toList());
    }

    private List<Resource> calculateResourcesForDeployment(ProcessContext context, DeploymentDescriptor deploymentDescriptor) {
        CloudModelBuilderContentCalculator<Resource> resourcesCloudModelBuilderContentCalculator = getResourcesCloudModelBuilderContentCalculator(context);

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

    private List<Module> calculateModulesForDeployment(ProcessContext context, DeploymentDescriptor deploymentDescriptor,
                                                       List<Module> mtaDescriptorModules, Set<String> mtaManifestModuleNames,
                                                       Set<String> deployedModuleNames, Set<String> mtaModuleNamesForDeployment) {
        CloudModelBuilderContentCalculator<Module> modulesCloudModelBuilderContentCalculator = getModulesContentCalculator(context,
                                                                                                                           mtaDescriptorModules,
                                                                                                                           mtaManifestModuleNames,
                                                                                                                           deployedModuleNames,
                                                                                                                           mtaModuleNamesForDeployment);
        return modulesCloudModelBuilderContentCalculator.calculateContentForBuilding(getModulesForDeployment(context.getExecution(),
                                                                                                             deploymentDescriptor));
    }

    private List<Resource>
            calculateResourcesForDeployment(DeploymentDescriptor deploymentDescriptor,
                                            CloudModelBuilderContentCalculator<Resource> resourcesCloudModelBuilderContentCalculator) {
        return resourcesCloudModelBuilderContentCalculator.calculateContentForBuilding(deploymentDescriptor.getResources());
    }

    private CloudModelBuilderContentCalculator<Resource> getResourcesCloudModelBuilderContentCalculator(ProcessContext context) {
        List<String> resourcesSpecifiedForDeployment = context.getVariable(Variables.RESOURCES_FOR_DEPLOYMENT);
        return new ResourcesCloudModelBuilderContentCalculator(resourcesSpecifiedForDeployment, getStepLogger());
    }

    protected ModulesCloudModelBuilderContentCalculator
              getModulesContentCalculator(ProcessContext context, List<Module> mtaDescriptorModules, Set<String> mtaManifestModuleNames,
                                          Set<String> deployedModuleNames, Set<String> mtaModuleNamesForDeployment) {
        List<ModulesContentValidator> modulesValidators = getModuleContentValidators(context.getControllerClient(), mtaDescriptorModules,
                                                                                     mtaModuleNamesForDeployment, deployedModuleNames);
        return new ModulesCloudModelBuilderContentCalculator(mtaManifestModuleNames,
                                                             deployedModuleNames,
                                                             context.getVariable(Variables.MODULES_FOR_DEPLOYMENT),
                                                             getStepLogger(),
                                                             moduleToDeployHelper,
                                                             modulesValidators);
    }

    private List<ModulesContentValidator> getModuleContentValidators(CloudControllerClient cloudControllerClient,
                                                                     List<Module> mtaDescriptorModules, Set<String> mtaModulesForDeployment,
                                                                     Set<String> deployedModuleNames) {
        return List.of(new UnresolvedModulesContentValidator(mtaModulesForDeployment, deployedModuleNames),
                       new DeployedAfterModulesContentValidator(cloudControllerClient,
                                                                getStepLogger(),
                                                                moduleToDeployHelper,
                                                                mtaDescriptorModules));
    }

    private List<? extends Module> getModulesForDeployment(DelegateExecution execution, DeploymentDescriptor deploymentDescriptor) {
        HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(execution);
        DescriptorHandler handler = handlerFactory.getDescriptorHandler();
        return handler.getModulesForDeployment(deploymentDescriptor, SupportedParameters.ENABLE_PARALLEL_DEPLOYMENTS,
                                               SupportedParameters.DEPENDENCY_TYPE,
                                               org.cloudfoundry.multiapps.controller.core.Constants.DEPENDENCY_TYPE_HARD);
    }

    protected ApplicationCloudModelBuilder getApplicationCloudModelBuilder(ProcessContext context) {
        return StepsUtil.getApplicationCloudModelBuilder(context);
    }

    protected ServicesCloudModelBuilder getServicesCloudModelBuilder(ProcessContext context) {
        CloudHandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context.getExecution());
        DeploymentDescriptor deploymentDescriptor = context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR);
        String namespace = context.getVariable(Variables.MTA_NAMESPACE);

        return handlerFactory.getServicesCloudModelBuilder(deploymentDescriptor, namespace);
    }

    protected ServiceKeysCloudModelBuilder getServiceKeysCloudModelBuilder(ProcessContext context) {
        CloudHandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context.getExecution());
        DeploymentDescriptor deploymentDescriptor = context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR);
        return handlerFactory.getServiceKeysCloudModelBuilder(deploymentDescriptor);
    }

    private List<String> getDomainsFromApps(ProcessContext context, DeploymentDescriptor descriptor,
                                            ApplicationCloudModelBuilder applicationCloudModelBuilder, List<? extends Module> modules,
                                            ModuleToDeployHelper moduleToDeployHelper) {
        Set<String> domains = new TreeSet<>();
        for (Module module : modules) {
            if (!moduleToDeployHelper.isApplication(module)) {
                continue;
            }
            ParametersChainBuilder parametersChainBuilder = new ParametersChainBuilder(context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR));
            List<Map<String, Object>> parametersList = parametersChainBuilder.buildModuleChain(module.getName());

            boolean noRoute = (Boolean) PropertiesUtil.getPropertyValue(parametersList, SupportedParameters.NO_ROUTE, false);
            if (!noRoute && context.getVariable(Variables.MISSING_DEFAULT_DOMAIN)) {
                throw new SLException(Messages.ERROR_MISSING_DEFAULT_DOMAIN);
            }

            List<String> appDomains = applicationCloudModelBuilder.getApplicationDomains(parametersList, module);
            if (appDomains != null) {
                domains.addAll(appDomains);
            }
        }

        String defaultDomain = (String) descriptor.getParameters()
                                                  .get(SupportedParameters.DEFAULT_DOMAIN);
        if (defaultDomain != null) {
            domains.remove(defaultDomain);
        }

        return new ArrayList<>(domains);
    }

}
