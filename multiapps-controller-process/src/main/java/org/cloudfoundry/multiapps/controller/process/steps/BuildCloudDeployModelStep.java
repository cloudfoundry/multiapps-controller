package org.cloudfoundry.multiapps.controller.process.steps;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.collections4.ListUtils;
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
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaServiceKey;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerialization;
import org.cloudfoundry.multiapps.controller.core.util.CloudModelBuilderUtil;
import org.cloudfoundry.multiapps.controller.core.util.NameUtil;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.builders.v2.ParametersChainBuilder;
import org.cloudfoundry.multiapps.mta.handlers.HandlerFactory;
import org.cloudfoundry.multiapps.mta.handlers.v2.DescriptorHandler;
import org.cloudfoundry.multiapps.mta.handlers.v2.ResourceBatchCalculator;
import org.cloudfoundry.multiapps.mta.handlers.v2.SelectiveDeployChecker;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.RequiredDependency;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.cloudfoundry.multiapps.mta.util.PropertiesUtil;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.util.CollectionUtils;

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
                                                                 .collect(toList());
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

        List<DeployedMtaServiceKey> serviceKeysToDelete = getServiceKeysToDelete(serviceKeys, deploymentDescriptor.getResources(), context);

        context.setVariable(Variables.SERVICE_KEYS_TO_DELETE, serviceKeysToDelete);

        ServicesCloudModelBuilder servicesCloudModelBuilder = getServicesCloudModelBuilder(context);

        // Build a list of services for binding and save them in the context:
        List<CloudServiceInstanceExtended> servicesForBindings = buildServicesForBindings(servicesCloudModelBuilder, deploymentDescriptor,
                                                                                          modulesCalculatedForDeployment);
        context.setVariable(Variables.SERVICES_TO_BIND, servicesForBindings);

        List<Resource> resourcesForDeployment = calculateResourcesForDeployment(context, deploymentDescriptor);

        SelectiveDeployChecker selectiveDeployChecker = getSelectiveDeployChecker(context, deploymentDescriptor);
        selectiveDeployChecker.check(resourcesForDeployment);
        getStepLogger().debug(Messages.CALCULATING_RESOURCE_BATCHES);

        List<List<Resource>> batchesToProcess = getResourceBatches(context, resourcesForDeployment);
        context.setVariable(Variables.BATCHES_TO_PROCESS, batchesToProcess);

        getStepLogger().debug(Messages.CALCULATING_RESOURCE_BATCHES_COMPLETE);

        getStepLogger().debug(Messages.CLOUD_MODEL_BUILT);
        return StepPhase.DONE;
    }

    private SelectiveDeployChecker getSelectiveDeployChecker(ProcessContext context, DeploymentDescriptor descriptor) {
        CloudHandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context.getExecution());
        DescriptorHandler descriptorHandler = handlerFactory.getDescriptorHandler();
        return handlerFactory.getSelectiveDeployChecker(descriptor, descriptorHandler);
    }

    private ResourceBatchCalculator getResourceBatchCalculator(ProcessContext context, DeploymentDescriptor descriptor) {
        CloudHandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context.getExecution());
        return handlerFactory.getResourceBatchCalculator(descriptor);
    }

    private List<List<Resource>> getResourceBatches(ProcessContext context, List<Resource> resources) {
        if (resources.isEmpty()) {
            return Collections.emptyList();
        }
        DeploymentDescriptor deploymentDescriptor = context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR);
        ResourceBatchCalculator resourceBatchCalculator = getResourceBatchCalculator(context, deploymentDescriptor);
        return new ArrayList<>(resourceBatchCalculator.groupResourcesByWeight(resources)
                                                      .values());
    }

    private List<DeployedMtaServiceKey> getServiceKeysToDelete(Map<String, List<CloudServiceKey>> serviceKeys, List<Resource> resources,
                                                               ProcessContext context) {
        getStepLogger().debug(Messages.DETECTING_SERVICE_KEYS_FOR_DELETION);
        List<DeployedMtaServiceKey> deployedServiceKeys = context.getVariable(Variables.DEPLOYED_MTA_SERVICE_KEYS);

        if (deployedServiceKeys == null || deployedServiceKeys.isEmpty()) {
            getStepLogger().debug(Messages.NO_SERVICE_KEYS_FOR_DELETION);
            return Collections.emptyList();
        }

        Map<String, List<DeployedMtaServiceKey>> deployedServiceKeysByService = deployedServiceKeys.stream()
                                                                                                   .collect(groupingBy(key -> key.getServiceInstance()
                                                                                                                                 .getName()));
        getStepLogger().debug(Messages.DEPLOYED_SERVICE_KEYS, deployedServiceKeysByService);

        Map<String, List<CloudServiceKey>> additionalServiceKeys = context.getVariable(Variables.SERVICE_KEYS_FOR_CONTENT_DEPLOY);
        Map<String, List<String>> newKeyNamesByService = mapKeysByServiceName(serviceKeys, additionalServiceKeys);
        getStepLogger().debug(Messages.NEW_SERVICE_KEYS, newKeyNamesByService);

        Map<String, List<String>> existingKeysByService = getExistingServiceKeysByServiceName(resources);
        getStepLogger().debug(Messages.EXISTING_SERVICE_KEYS, existingKeysByService);

        List<DeployedMtaServiceKey> keysToDelete = new ArrayList<>();

        for (Entry<String, List<DeployedMtaServiceKey>> deployedKeys : deployedServiceKeysByService.entrySet()) {
            String serviceName = deployedKeys.getKey();

            List<String> newManagedKeys = newKeyNamesByService.getOrDefault(serviceName, Collections.emptyList());
            List<String> existingKeys = existingKeysByService.getOrDefault(serviceName, Collections.emptyList());

            List<DeployedMtaServiceKey> unusedKeys = deployedKeys.getValue()
                                                                 .stream()
                                                                 .filter(deployedKey -> keyIsMissingInBothLists(deployedKey.getName(),
                                                                                                                newManagedKeys,
                                                                                                                existingKeys))
                                                                 .collect(toList());

            keysToDelete.addAll(unusedKeys);
        }

        getStepLogger().debug(Messages.SERVICE_KEYS_FOR_DELETION, keysToDelete);
        return keysToDelete;

    }

    private boolean keyIsMissingInBothLists(String keyName, List<String> firstList, List<String> secondList) {
        return !(firstList.contains(keyName) || secondList.contains(keyName));
    }

    private Map<String, List<String>> mapKeysByServiceName(Map<String, List<CloudServiceKey>> keysByResource,
                                                           Map<String, List<CloudServiceKey>> additionalKeysByResource) {
        Map<String, List<String>> keyNamesMap = new HashMap<>();

        addKeysNamesToMap(keyNamesMap, keysByResource);
        addKeysNamesToMap(keyNamesMap, additionalKeysByResource);

        return keyNamesMap;
    }

    private void addKeysNamesToMap(Map<String, List<String>> allKeysMap, Map<String, List<CloudServiceKey>> keysByResource) {
        if (keysByResource == null || keysByResource.isEmpty()) {
            return;
        }

        for (List<CloudServiceKey> keysForResource : keysByResource.values()) {
            if (!CollectionUtils.isEmpty(keysForResource)) {
                addServiceKeysToMap(keysForResource, allKeysMap);
            }
        }
    }

    private void addServiceKeysToMap(List<CloudServiceKey> keysForResource, Map<String, List<String>> allKeysMappedByService) {
        String serviceName = keysForResource.get(0)
                                            .getServiceInstance()
                                            .getName();

        List<String> keyNames = keysForResource.stream()
                                               .map(key -> key.getName())
                                               .collect(toList());

        allKeysMappedByService.merge(serviceName, keyNames, ListUtils::union);
    }

    private Map<String, List<String>> getExistingServiceKeysByServiceName(List<Resource> resources) {

        return resources.stream()
                        .filter(CloudModelBuilderUtil::isExistingServiceKey)
                        .collect(groupingBy(this::getServiceName, mapping(this::getServiceKeyName, toList())));
    }

    private String getServiceName(Resource resource) {
        return (String) resource.getParameters()
                                .get(SupportedParameters.SERVICE_NAME);
    }

    private String getServiceKeyName(Resource resource) {
        return (String) resource.getParameters()
                                .getOrDefault(SupportedParameters.SERVICE_KEY_NAME, resource.getName());
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_BUILDING_CLOUD_MODEL;
    }

    private List<String> getAppNames(List<Module> modulesCalculatedForDeployment) {
        return modulesCalculatedForDeployment.stream()
                                             .filter(moduleToDeployHelper::isApplication)
                                             .map(NameUtil::getApplicationName)
                                             .collect(toList());
    }

    private List<CloudServiceInstanceExtended> buildServicesForBindings(ServicesCloudModelBuilder servicesCloudModelBuilder,
                                                                        DeploymentDescriptor deploymentDescriptor,
                                                                        List<Module> modulesCalculatedForDeployment) {
        List<String> resourcesRequiredByModules = calculateResourceNamesRequiredByModules(modulesCalculatedForDeployment,
                                                                                          deploymentDescriptor.getResources());
        return buildFilteredServices(deploymentDescriptor, resourcesRequiredByModules, servicesCloudModelBuilder);
    }

    private List<CloudServiceInstanceExtended> buildFilteredServices(DeploymentDescriptor deploymentDescriptor,
                                                                     List<String> filteredResourceNames,
                                                                     ServicesCloudModelBuilder servicesCloudModelBuilder) {
        CloudModelBuilderContentCalculator<Resource> resourcesCloudModelBuilderContentCalculator = new ResourcesCloudModelBuilderContentCalculator(filteredResourceNames,
                                                                                                                                                   getStepLogger());
        // this always filters the 'isActive', 'isResourceSpecifiedForDeployment' and 'isService' resources
        List<Resource> calculatedFilteredResources = resourcesCloudModelBuilderContentCalculator.calculateContentForBuilding(deploymentDescriptor.getResources());

        return servicesCloudModelBuilder.build(calculatedFilteredResources);
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
                      .collect(toSet());
    }

    private List<String> getDependencyNamesToResources(List<Resource> resources, Set<String> requiredDependencies) {
        return resources.stream()
                        .map(Resource::getName)
                        .filter(requiredDependencies::contains)
                        .collect(toList());
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

    private List<Resource> calculateResourcesForDeployment(ProcessContext context, DeploymentDescriptor deploymentDescriptor) {
        List<String> resourcesSpecifiedForDeployment = context.getVariable(Variables.RESOURCES_FOR_DEPLOYMENT);
        CloudModelBuilderContentCalculator<Resource> resourcesCloudModelBuilderContentCalculator = new ResourcesCloudModelBuilderContentCalculator(resourcesSpecifiedForDeployment,
                                                                                                                                                   getStepLogger());
        return resourcesCloudModelBuilderContentCalculator.calculateContentForBuilding(deploymentDescriptor.getResources());
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
        String namespace = context.getVariable(Variables.MTA_NAMESPACE);
        String spaceGuid = context.getVariable(Variables.SPACE_GUID);

        return handlerFactory.getServiceKeysCloudModelBuilder(deploymentDescriptor, namespace, spaceGuid);
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
