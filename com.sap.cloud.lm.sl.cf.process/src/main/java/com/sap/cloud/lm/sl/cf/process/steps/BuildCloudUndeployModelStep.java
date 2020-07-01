package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.collections4.CollectionUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceInstanceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ApplicationCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.helpers.ModuleToDeployHelper;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaService;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationSubscriptionService;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerialization;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Module;

@Named("buildCloudUndeployModelStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class BuildCloudUndeployModelStep extends SyncFlowableStep {

    @Inject
    private ConfigurationSubscriptionService configurationSubscriptionService;
    @Inject
    private ModuleToDeployHelper moduleToDeployHelper;

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.BUILDING_CLOUD_UNDEPLOY_MODEL);
        DeployedMta deployedMta = context.getVariable(Variables.DEPLOYED_MTA);

        if (deployedMta == null) {
            setComponentsToUndeploy(context, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
            return StepPhase.DONE;
        }

        List<String> deploymentDescriptorModules = getDeploymentDescriptorModules(context);
        List<ConfigurationSubscription> subscriptionsToCreate = context.getVariable(Variables.SUBSCRIPTIONS_TO_CREATE);
        Set<String> mtaModules = context.getVariable(Variables.MTA_MODULES);
        List<String> appNames = context.getVariable(Variables.APPS_TO_DEPLOY);
        List<String> serviceNames = getServicesToCreate(context);

        getStepLogger().debug(Messages.MTA_MODULES, mtaModules);

        List<DeployedMtaApplication> deployedAppsToUndeploy = computeModulesToUndeploy(deployedMta, mtaModules, appNames,
                                                                                       deploymentDescriptorModules);
        getStepLogger().debug(Messages.MODULES_TO_UNDEPLOY, SecureSerialization.toJson(deployedAppsToUndeploy));

        List<DeployedMtaApplication> appsWithoutChange = computeModulesWithoutChange(deployedAppsToUndeploy, mtaModules, deployedMta);
        getStepLogger().debug(Messages.MODULES_NOT_TO_BE_CHANGED, SecureSerialization.toJson(appsWithoutChange));

        List<ConfigurationSubscription> subscriptionsToDelete = computeSubscriptionsToDelete(subscriptionsToCreate, deployedMta,
                                                                                             context.getVariable(Variables.SPACE_GUID));
        getStepLogger().debug(Messages.SUBSCRIPTIONS_TO_DELETE, SecureSerialization.toJson(subscriptionsToDelete));

        Set<String> servicesForApplications = getServicesForApplications(context);
        List<String> servicesToDelete = computeServicesToDelete(appsWithoutChange, deployedMta.getServices(), servicesForApplications,
                                                                serviceNames);
        getStepLogger().debug(Messages.SERVICES_TO_DELETE, servicesToDelete);

        List<CloudApplication> appsToUndeploy = computeAppsToUndeploy(deployedAppsToUndeploy, context.getControllerClient());
        getStepLogger().debug(Messages.APPS_TO_UNDEPLOY, SecureSerialization.toJson(appsToUndeploy));

        setComponentsToUndeploy(context, servicesToDelete, appsToUndeploy, subscriptionsToDelete);

        getStepLogger().debug(Messages.CLOUD_UNDEPLOY_MODEL_BUILT);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_BUILDING_CLOUD_UNDEPLOY_MODEL;
    }

    private List<String> getDeploymentDescriptorModules(ProcessContext context) {
        DeploymentDescriptor deploymentDescriptor = context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR);
        if (deploymentDescriptor == null) {
            return Collections.emptyList();
        }
        return deploymentDescriptor.getModules()
                                   .stream()
                                   .map(Module::getName)
                                   .collect(Collectors.toList());
    }

    private List<String> getServicesToCreate(ProcessContext context) {
        return context.getVariable(Variables.SERVICES_TO_CREATE)
                      .stream()
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

    private List<DeployedMtaApplication> computeModulesWithoutChange(List<DeployedMtaApplication> modulesToUndeploy, Set<String> mtaModules,
                                                                     DeployedMta deployedMta) {
        return deployedMta.getApplications()
                          .stream()
                          .filter(existingModule -> shouldNotUndeployModule(modulesToUndeploy, existingModule))
                          .filter(existingModule -> shouldNotDeployModule(mtaModules, existingModule))
                          .collect(Collectors.toList());
    }

    private boolean shouldNotUndeployModule(List<DeployedMtaApplication> modulesToUndeploy, DeployedMtaApplication existingModule) {
        String existingModuleName = existingModule.getModuleName();
        return modulesToUndeploy.stream()
                                .map(DeployedMtaApplication::getModuleName)
                                .noneMatch(existingModuleName::equals);
    }

    private boolean shouldNotDeployModule(Set<String> mtaModules, DeployedMtaApplication existingModule) {
        String existingModuleName = existingModule.getModuleName();
        return !mtaModules.contains(existingModuleName);
    }

    private void setComponentsToUndeploy(ProcessContext context, List<String> services, List<CloudApplication> apps,
                                         List<ConfigurationSubscription> subscriptions) {
        context.setVariable(Variables.SUBSCRIPTIONS_TO_DELETE, subscriptions);
        context.setVariable(Variables.SERVICES_TO_DELETE, services);
        context.setVariable(Variables.APPS_TO_UNDEPLOY, apps);
    }

    private List<String> computeServicesToDelete(List<DeployedMtaApplication> appsWithoutChange,
                                                 List<DeployedMtaService> deployedMtaServices, Set<String> servicesForApplications,
                                                 List<String> servicesForCurrentDeployment) {
        return deployedMtaServices.stream()
                                  .map(DeployedMtaService::getName)
                                  .filter(service -> shouldDeleteService(service, appsWithoutChange, servicesForApplications,
                                                                         servicesForCurrentDeployment))
                                  .sorted()
                                  .collect(Collectors.toList());
    }

    private boolean shouldDeleteService(String service, List<DeployedMtaApplication> appsToKeep, Set<String> servicesForApplications,
                                        List<String> servicesForCurrentDeployment) {
        return appsToKeep.stream()
                         .flatMap(module -> module.getBoundMtaServices()
                                                  .stream())
                         .noneMatch(service::equalsIgnoreCase)
            && !servicesForApplications.contains(service) && !servicesForCurrentDeployment.contains(service);
    }

    private List<DeployedMtaApplication> computeModulesToUndeploy(DeployedMta deployedMta, Set<String> mtaModules,
                                                                  List<String> appsToDeploy, List<String> deploymentDescriptorModules) {
        return deployedMta.getApplications()
                          .stream()
                          .filter(deployedApplication -> shouldBeCheckedForUndeployment(deployedApplication, mtaModules,
                                                                                        deploymentDescriptorModules))
                          .filter(deployedApplication -> shouldUndeployModule(deployedApplication, appsToDeploy))
                          .collect(Collectors.toList());
    }

    private boolean shouldBeCheckedForUndeployment(DeployedMtaApplication deployedApplication, Set<String> mtaModules,
                                                   List<String> deploymentDescriptorModules) {
        return mtaModules.contains(deployedApplication.getModuleName())
            || !deploymentDescriptorModules.contains(deployedApplication.getModuleName());
    }

    private boolean shouldUndeployModule(DeployedMtaApplication deployedMtaApplication, List<String> appsToDeploy) {
        // The deployed module may be in the list of MTA modules, but the actual application that was created from it may have a
        // different name:
        return !appsToDeploy.contains(deployedMtaApplication.getName());
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
        return Objects.equals(subscription1.getAppName(), subscription2.getAppName())
            && Objects.equals(subscription1.getSpaceId(), subscription2.getSpaceId()) && Objects.equals(subscription1.getResourceDto()
                                                                                                                     .getName(),
                                                                                                        subscription2.getResourceDto()
                                                                                                                     .getName());
    }

    protected ApplicationCloudModelBuilder getApplicationCloudModelBuilder(ProcessContext context) {
        return StepsUtil.getApplicationCloudModelBuilder(context);
    }

}
