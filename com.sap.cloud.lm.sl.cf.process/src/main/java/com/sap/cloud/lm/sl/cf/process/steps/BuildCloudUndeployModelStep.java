package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationSubscriptionDao;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;

@Component("buildCloudUndeployModelStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class BuildCloudUndeployModelStep extends SyncFlowableStep {

    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Inject
    private ConfigurationSubscriptionDao dao;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        getStepLogger().debug(Messages.BUILDING_CLOUD_UNDEPLOY_MODEL);
        try {
            DeployedMta deployedMta = StepsUtil.getDeployedMta(execution.getContext());

            List<String> deploymentDescriptorModules = getDeploymentDescriptorModules(execution.getContext());

            if (deployedMta == null) {
                setComponentsToUndeploy(execution.getContext(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
                return StepPhase.DONE;
            }

            List<ConfigurationSubscription> subscriptionsToCreate = StepsUtil.getSubscriptionsToCreate(execution.getContext());
            Set<String> mtaModules = StepsUtil.getMtaModules(execution.getContext());
            List<CloudApplicationExtended> appsToDeploy = StepsUtil.getAppsToDeploy(execution.getContext());
            List<CloudApplication> deployedApps = StepsUtil.getDeployedApps(execution.getContext());

            getStepLogger().debug(Messages.MTA_MODULES, mtaModules);

            List<String> appNames = appsToDeploy.stream()
                .map(CloudApplication::getName)
                .collect(Collectors.toList());

            List<DeployedMtaModule> modulesToUndeploy = computeModulesToUndeploy(deployedMta, mtaModules, appNames,
                deploymentDescriptorModules);
            getStepLogger().debug(Messages.MODULES_TO_UNDEPLOY, secureSerializer.toJson(modulesToUndeploy));

            List<DeployedMtaModule> modulesWithoutChange = computeModulesWithoutChange(modulesToUndeploy, mtaModules, deployedMta);
            getStepLogger().debug(Messages.MODULES_NOT_TO_BE_CHANGED, secureSerializer.toJson(modulesWithoutChange));

            List<ConfigurationSubscription> subscriptionsToDelete = computeSubscriptionsToDelete(subscriptionsToCreate, deployedMta,
                StepsUtil.getSpaceId(execution.getContext()));
            getStepLogger().debug(Messages.SUBSCRIPTIONS_TO_DELETE, secureSerializer.toJson(subscriptionsToDelete));

            List<String> servicesToDelete = computeServicesToDelete(modulesWithoutChange, deployedMta.getServices(), appsToDeploy);
            getStepLogger().debug(Messages.SERVICES_TO_DELETE, servicesToDelete);

            List<CloudApplication> appsToUndeploy = computeAppsToUndeploy(modulesToUndeploy, deployedApps);
            getStepLogger().debug(Messages.APPS_TO_UNDEPLOY, secureSerializer.toJson(appsToUndeploy));

            setComponentsToUndeploy(execution.getContext(), servicesToDelete, appsToUndeploy, subscriptionsToDelete);

            getStepLogger().debug(Messages.CLOUD_UNDEPLOY_MODEL_BUILT);
            return StepPhase.DONE;
        } catch (Exception e) {
            getStepLogger().error(e, Messages.ERROR_BUILDING_CLOUD_UNDEPLOY_MODEL);
            throw e;
        }
    }

    private List<String> getDeploymentDescriptorModules(DelegateExecution context) {
        DeploymentDescriptor deploymentDescriptor = StepsUtil.getDeploymentDescriptor(context);
        if (deploymentDescriptor == null) {
            return Collections.emptyList();
        }
        return deploymentDescriptor.getModules2()
            .stream()
            .map(module -> module.getName())
            .collect(Collectors.toList());
    }

    private List<DeployedMtaModule> computeModulesWithoutChange(List<DeployedMtaModule> modulesToUndeploy, Set<String> mtaModules,
        DeployedMta deployedMta) {
        return deployedMta.getModules()
            .stream()
            .filter(existingModule -> shouldNotUndeployModule(modulesToUndeploy, existingModule))
            .filter(existingModule -> shouldNotDeployModule(mtaModules, existingModule))
            .collect(Collectors.toList());
    }

    private boolean shouldNotUndeployModule(List<DeployedMtaModule> modulesToUndeploy, DeployedMtaModule existingModule) {
        String existingModuleName = existingModule.getModuleName();
        return modulesToUndeploy.stream()
            .map(DeployedMtaModule::getModuleName)
            .noneMatch(moduleName -> existingModuleName.equals(moduleName));
    }

    private boolean shouldNotDeployModule(Set<String> mtaModules, DeployedMtaModule existingModule) {
        String existingModuleName = existingModule.getModuleName();
        return mtaModules.stream()
            .noneMatch(moduleName -> existingModuleName.equals(moduleName));
    }

    private void setComponentsToUndeploy(DelegateExecution context, List<String> services, List<CloudApplication> apps,
        List<ConfigurationSubscription> subscriptions) {
        StepsUtil.setSubscriptionsToDelete(context, subscriptions);
        StepsUtil.setServicesToDelete(context, services);
        StepsUtil.setAppsToUndeploy(context, apps);
    }

    private List<String> computeServicesToDelete(List<DeployedMtaModule> modulesWithoutChange, Set<String> existingServices,
        List<CloudApplicationExtended> appsToDeploy) {
        return existingServices.stream()
            .filter(service -> shouldDeleteService(modulesWithoutChange, service, appsToDeploy))
            .collect(Collectors.toList());
    }

    private boolean shouldDeleteService(List<DeployedMtaModule> modulesToKeep, String service,
        List<CloudApplicationExtended> appsToDeploy) {
        return modulesToKeep.stream()
            .map(DeployedMtaModule::getServices)
            .noneMatch(moduleToKeepService -> moduleToKeepService.contains(service))
            && appsToDeploy.stream()
                .map(CloudApplicationExtended::getServices)
                .noneMatch(appService -> appService.contains(service));
    }

    private List<DeployedMtaModule> computeModulesToUndeploy(DeployedMta deployedMta, Set<String> mtaModules, List<String> appsToDeploy,
        List<String> deploymentDescriptorModules) {
        return deployedMta.getModules()
            .stream()
            .filter(deployedModule -> shouldBeCheckedforUndeployment(deployedModule, mtaModules, deploymentDescriptorModules))
            .filter(deployedModule -> shouldUndeployModule(deployedModule, mtaModules, appsToDeploy))
            .collect(Collectors.toList());
    }

    private boolean shouldBeCheckedforUndeployment(DeployedMtaModule deployedModule, Set<String> mtaModules,
        List<String> deploymentDescriptorModules) {
        return mtaModules.contains(deployedModule.getModuleName()) || !deploymentDescriptorModules.contains(deployedModule.getModuleName());
    }

    private boolean shouldUndeployModule(DeployedMtaModule deployedModule, Set<String> mtaModules, List<String> appsToDeploy) {
        // The deployed module may be in the list of MTA modules, but the actual application that was created from it may have a
        // different name:
        return !appsToDeploy.contains(deployedModule.getAppName());
    }

    private List<CloudApplication> computeAppsToUndeploy(List<DeployedMtaModule> modulesToUndeploy, List<CloudApplication> deployedApps) {
        return deployedApps.stream()
            .filter(app -> shouldUndeployApp(modulesToUndeploy, app))
            .collect(Collectors.toList());
    }

    private boolean shouldUndeployApp(List<DeployedMtaModule> modulesToUndeploy, CloudApplication app) {
        return modulesToUndeploy.stream()
            .anyMatch(module -> module.getAppName()
                .equals(app.getName()));
    }

    private List<ConfigurationSubscription> computeSubscriptionsToDelete(List<ConfigurationSubscription> subscriptionsToCreate,
        DeployedMta deployedMta, String spaceId) {
        String mtaId = deployedMta.getMetadata()
            .getId();
        List<ConfigurationSubscription> existingSubscriptions = dao.findAll(mtaId, null, spaceId, null);
        return existingSubscriptions.stream()
            .filter(subscription -> !willBeCreatedOrUpdated(subscription, subscriptionsToCreate))
            .collect(Collectors.toList());
    }

    private boolean willBeCreatedOrUpdated(ConfigurationSubscription existingSubscription,
        List<ConfigurationSubscription> createdOrUpdatedSubscriptions) {
        return createdOrUpdatedSubscriptions.stream()
            .anyMatch(subscription -> areEqual(subscription, existingSubscription));
    }

    protected boolean areEqual(ConfigurationSubscription subscription1, ConfigurationSubscription subscription2) {
        return Objects.equals(subscription1.getAppName(), subscription2.getAppName())
            && Objects.equals(subscription1.getSpaceId(), subscription2.getSpaceId()) && Objects.equals(subscription1.getResourceDto()
                .getName(),
                subscription2.getResourceDto()
                    .getName());
    }

}
