package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.cf.v2.ApplicationCloudModelBuilder;
import org.cloudfoundry.multiapps.controller.core.helpers.ModuleToDeployHelper;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaService;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaServiceKey;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerialization;
import org.cloudfoundry.multiapps.controller.core.util.CloudModelBuilderUtil;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationSubscription;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationSubscriptionService;
import org.cloudfoundry.multiapps.controller.persistence.services.DescriptorBackupService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ExistingAppsToBackupCalculator;
import org.cloudfoundry.multiapps.controller.process.util.ModulesToUndeployCalculator;
import org.cloudfoundry.multiapps.controller.process.util.ProcessTypeParser;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

@Named("buildCloudUndeployModelStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class BuildCloudUndeployModelStep extends SyncFlowableStep {

    @Inject
    private ConfigurationSubscriptionService configurationSubscriptionService;
    @Inject
    private ModuleToDeployHelper moduleToDeployHelper;
    @Inject
    private ProcessTypeParser processTypeParser;
    @Inject
    private DescriptorBackupService descriptorBackupService;

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.BUILDING_CLOUD_UNDEPLOY_MODEL);
        DeployedMta deployedMta = context.getVariable(Variables.DEPLOYED_MTA);

        List<DeployedMtaServiceKey> serviceKeysToDelete = computeServiceKeysToDelete(context);
        getStepLogger().debug(Messages.SERVICE_KEYS_FOR_DELETION, serviceKeysToDelete);

        if (deployedMta == null) {
            setComponentsToUndeploy(context, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                                    Collections.emptyList(), Collections.emptyList());

            if (!serviceKeysToDelete.isEmpty()) {
                context.setVariable(Variables.SERVICE_KEYS_TO_DELETE,
                                    getServiceKeysToDelete(context, serviceKeysToDelete));
            }
            return StepPhase.DONE;
        }

        List<Module> deploymentDescriptorModules = getDeploymentDescriptorModules(context);
        List<ConfigurationSubscription> subscriptionsToCreate = context.getVariable(Variables.SUBSCRIPTIONS_TO_CREATE);
        Set<String> mtaModules = context.getVariable(Variables.MTA_MODULES);
        List<String> appNames = context.getVariable(Variables.APPS_TO_DEPLOY);
        List<String> serviceNames = getAllServiceNames(context);

        getStepLogger().debug(Messages.MTA_MODULES, mtaModules);

        ModulesToUndeployCalculator modulesToUndeployCalculator = new ModulesToUndeployCalculator(deployedMta, mtaModules,
                                                                                                  deploymentDescriptorModules,
                                                                                                  moduleToDeployHelper);
        List<DeployedMtaApplication> deployedAppsToUndeploy = modulesToUndeployCalculator.computeModulesToUndeploy(appNames);

        getStepLogger().debug(Messages.MODULES_TO_UNDEPLOY, SecureSerialization.toJson(deployedAppsToUndeploy));

        List<DeployedMtaApplication> appsWithoutChange = modulesToUndeployCalculator.computeModulesWithoutChange(deployedAppsToUndeploy);
        getStepLogger().debug(Messages.MODULES_NOT_TO_BE_CHANGED, SecureSerialization.toJson(appsWithoutChange));

        List<ConfigurationSubscription> subscriptionsToDelete = computeSubscriptionsToDelete(subscriptionsToCreate, deployedMta,
                                                                                             context.getVariable(Variables.SPACE_GUID));
        getStepLogger().debug(Messages.SUBSCRIPTIONS_TO_DELETE, SecureSerialization.toJson(subscriptionsToDelete));

        Set<String> servicesForApplications = getServicesForApplications(context);
        List<String> servicesToDelete = computeServicesToDelete(context, appsWithoutChange, deployedMta.getServices(),
                                                                servicesForApplications, serviceNames);
        getStepLogger().debug(Messages.SERVICES_TO_DELETE, servicesToDelete);

        List<CloudApplication> appsToUndeploy = computeAppsToUndeploy(deployedAppsToUndeploy, context.getControllerClient());

        DeployedMta backupMta = context.getVariable(Variables.BACKUP_MTA);
        ExistingAppsToBackupCalculator existingAppsToBackupCalculator = new ExistingAppsToBackupCalculator(deployedMta, backupMta,
                                                                                                           descriptorBackupService);
        List<CloudApplication> existingAppsToBackup = computeExistingAppsToBackup(context, appsToUndeploy, existingAppsToBackupCalculator);

        List<CloudApplication> backupAppsToUndeploy = existingAppsToBackupCalculator.calculateAppsToUndeploy(context, existingAppsToBackup);

        appsToUndeploy.removeAll(existingAppsToBackup);
        appsToUndeploy.addAll(backupAppsToUndeploy);

        getStepLogger().debug(Messages.APPS_TO_UNDEPLOY, SecureSerialization.toJson(appsToUndeploy));

        setComponentsToUndeploy(context, servicesToDelete, appsToUndeploy, subscriptionsToDelete, serviceKeysToDelete,
                                existingAppsToBackup);

        getStepLogger().debug(Messages.CLOUD_UNDEPLOY_MODEL_BUILT);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_BUILDING_CLOUD_UNDEPLOY_MODEL;
    }

    private List<Module> getDeploymentDescriptorModules(ProcessContext context) {
        DeploymentDescriptor deploymentDescriptor = context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR);
        if (deploymentDescriptor == null) {
            return Collections.emptyList();
        }
        return deploymentDescriptor.getModules();
    }

    private List<String> getAllServiceNames(ProcessContext context) {
        return context.getVariableBackwardsCompatible(Variables.BATCHES_TO_PROCESS)
                      .stream()
                      .flatMap(List::stream)
                      .map(CloudServiceInstanceExtended::getName)
                      .collect(Collectors.toList());
    }

    private Set<String> getServicesForApplications(ProcessContext context) {
        List<Module> modules = context.getVariable(Variables.MODULES_TO_DEPLOY);
        if (CollectionUtils.isEmpty(modules)) {
            return Collections.emptySet();
        }
        Set<String> servicesForApplications = new HashSet<>();
        ApplicationCloudModelBuilder applicationCloudModelBuilder = getApplicationCloudModelBuilder(context);
        for (Module module : modules) {
            if (moduleToDeployHelper.isApplication(module)) {
                servicesForApplications.addAll(applicationCloudModelBuilder.getAllApplicationServices(module));
            }
        }
        return servicesForApplications;
    }

    private void setComponentsToUndeploy(ProcessContext context, List<String> services, List<CloudApplication> apps,
                                         List<ConfigurationSubscription> subscriptions, List<DeployedMtaServiceKey> serviceKeys,
                                         List<CloudApplication> appsToBackup) {
        context.setVariable(Variables.SUBSCRIPTIONS_TO_DELETE, subscriptions);
        context.setVariable(Variables.SERVICES_TO_DELETE, services);
        context.setVariable(Variables.APPS_TO_UNDEPLOY, apps);
        context.setVariable(Variables.SERVICE_KEYS_TO_DELETE, getServiceKeysToDelete(context, serviceKeys));
        context.setVariable(Variables.APPS_TO_BACKUP, appsToBackup);
    }

    private List<DeployedMtaServiceKey> getServiceKeysToDelete(ProcessContext context, List<DeployedMtaServiceKey> serviceKeys) {
        List<DeployedMtaServiceKey> scheduledServiceKeysForDeletion = context.getVariable(Variables.SERVICE_KEYS_TO_DELETE);
        return ListUtils.union(scheduledServiceKeysForDeletion, serviceKeys);
    }

    private List<String> computeServicesToDelete(ProcessContext context, List<DeployedMtaApplication> appsWithoutChange,
                                                 List<DeployedMtaService> deployedMtaServices, Set<String> servicesForApplications,
                                                 List<String> servicesForCurrentDeployment) {
        return deployedMtaServices.stream()
                                  .filter(service -> shouldDeleteService(context, service, appsWithoutChange, servicesForApplications,
                                                                         servicesForCurrentDeployment))
                                  .map(DeployedMtaService::getName)
                                  .sorted()
                                  .collect(Collectors.toList());
    }

    private boolean shouldDeleteService(ProcessContext context, DeployedMtaService service, List<DeployedMtaApplication> appsToKeep,
                                        Set<String> servicesForApplications, List<String> servicesForCurrentDeployment) {
        if (isExistingService(context, service.getName())) {
            // service, whose type was changed from "managed" to "existing"
            // flag to "delete" as we read that for DetachServicesFromMta step
            return true;
        }
        return appsToKeep.stream()
                         .flatMap(module -> module.getBoundMtaServices()
                                                  .stream())
                         .noneMatch(serviceName -> serviceName.equalsIgnoreCase(service.getName())) && !servicesForApplications.contains(
            service.getName()) && !servicesForCurrentDeployment.contains(service.getName());
    }

    private boolean isExistingService(ProcessContext context, String serviceName) {
        var deploymentDescriptor = context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR);
        // the process type is checked because the deployment descriptor is null during undeployment
        return !ProcessType.UNDEPLOY.equals(processTypeParser.getProcessType(context.getExecution()))
            && CloudModelBuilderUtil.isExistingService(deploymentDescriptor.getResources(), serviceName);
    }

    private List<DeployedMtaServiceKey> computeServiceKeysToDelete(ProcessContext context) {
        getStepLogger().debug(Messages.DETECTING_SERVICE_KEYS_FOR_DELETION);
        List<DeployedMtaServiceKey> deployedServiceKeys = context.getVariable(Variables.DEPLOYED_MTA_SERVICE_KEYS);

        if (deployedServiceKeys == null || deployedServiceKeys.isEmpty()) {
            getStepLogger().debug(Messages.NO_SERVICE_KEYS_FOR_DELETION);
            return Collections.emptyList();
        }

        if (ProcessType.UNDEPLOY.equals(processTypeParser.getProcessType(context.getExecution()))) {
            if (StepsUtil.canDeleteServiceKeys(context)) {
                return deployedServiceKeys;
            } else {
                getStepLogger().debug(Messages.WILL_NOT_DELETE_SERVICE_KEYS);
                return Collections.emptyList();
            }
        }

        return computeUnusedServiceKeys(context, deployedServiceKeys);

    }

    private List<DeployedMtaServiceKey> computeUnusedServiceKeys(ProcessContext context, List<DeployedMtaServiceKey> deployedServiceKeys) {
        Map<String, List<DeployedMtaServiceKey>> deployedServiceKeysByService = deployedServiceKeys.stream()
                                                                                                   .collect(groupingBy(
                                                                                                       key -> key.getServiceInstance()
                                                                                                                 .getName()));
        getStepLogger().debug(Messages.DEPLOYED_SERVICE_KEYS, deployedServiceKeysByService);

        Map<String, List<CloudServiceKey>> additionalServiceKeys = context.getVariable(Variables.SERVICE_KEYS_FOR_CONTENT_DEPLOY);
        Map<String, List<CloudServiceKey>> serviceKeysToCreate = context.getVariable(Variables.SERVICE_KEYS_TO_CREATE);
        Map<String, List<String>> newKeyNamesByService = mapKeysByServiceName(serviceKeysToCreate, additionalServiceKeys);
        getStepLogger().debug(Messages.NEW_SERVICE_KEYS, newKeyNamesByService);

        DeploymentDescriptor deploymentDescriptor = context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR);
        Map<String, List<String>> existingKeysByService = getExistingServiceKeysByServiceName(deploymentDescriptor.getResources());
        getStepLogger().debug(Messages.EXISTING_SERVICE_KEYS_BY_SERVICE, existingKeysByService);

        List<DeployedMtaServiceKey> unusedKeysForAllServices = new ArrayList<>();

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

            unusedKeysForAllServices.addAll(unusedKeys);
        }

        return unusedKeysForAllServices;
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

    private List<CloudApplication> computeExistingAppsToBackup(ProcessContext context, List<CloudApplication> appsToUndeploy,
                                                               ExistingAppsToBackupCalculator existingAppsToBackupCalculator) {
        boolean shouldBackupExistingApps = context.getVariable(Variables.SHOULD_BACKUP_PREVIOUS_VERSION);
        if (!shouldBackupExistingApps) {
            return Collections.emptyList();
        }
        String mtaVersionOfCurrentDescriptor = context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR)
                                                      .getVersion();
        return existingAppsToBackupCalculator.calculateExistingAppsToBackup(context, appsToUndeploy, mtaVersionOfCurrentDescriptor);
    }

    private List<CloudApplication> computeAppsToUndeploy(List<DeployedMtaApplication> modulesToUndeploy, CloudControllerClient client) {
        return modulesToUndeploy.stream()
                                .map(appToUndeploy -> client.getApplication(appToUndeploy.getName(), false))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
    }

    private List<ConfigurationSubscription> computeSubscriptionsToDelete(List<ConfigurationSubscription> subscriptionsToCreate,
                                                                         DeployedMta deployedMta, String spaceId) {
        String mtaId = deployedMta.getMetadata()
                                  .getId();
        List<ConfigurationSubscription> existingSubscriptions = configurationSubscriptionService.createQuery()
                                                                                                .mtaId(mtaId)
                                                                                                .spaceId(spaceId)
                                                                                                .list();
        return existingSubscriptions.stream()
                                    .filter(subscription -> !willBeCreatedOrUpdated(subscription, subscriptionsToCreate))
                                    .collect(Collectors.toList());
    }

    private boolean willBeCreatedOrUpdated(ConfigurationSubscription existingSubscription,
                                           List<ConfigurationSubscription> createdOrUpdatedSubscriptions) {
        return createdOrUpdatedSubscriptions.stream()
                                            .anyMatch(subscription -> areEqual(subscription, existingSubscription));
    }

    private boolean areEqual(ConfigurationSubscription subscription1, ConfigurationSubscription subscription2) {
        return Objects.equals(subscription1.getAppName(), subscription2.getAppName()) && Objects.equals(subscription1.getSpaceId(),
                                                                                                        subscription2.getSpaceId())
            && Objects.equals(subscription1.getResourceDto()
                                           .getName(), subscription2.getResourceDto()
                                                                    .getName());
    }

    protected ApplicationCloudModelBuilder getApplicationCloudModelBuilder(ProcessContext context) {
        return StepsUtil.getApplicationCloudModelBuilder(context);
    }

}
