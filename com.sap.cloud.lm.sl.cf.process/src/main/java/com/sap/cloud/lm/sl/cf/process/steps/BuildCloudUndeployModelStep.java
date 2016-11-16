package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationSubscriptionDao;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("buildCloudUndeployModelStep")
public class BuildCloudUndeployModelStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(BuildCloudUndeployModelStep.class);

    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Inject
    private ConfigurationSubscriptionDao dao;

    public static StepMetadata getMetadata() {
        return new StepMetadata("buildUndeployModelTask", "Build Undeploy Model", "Build Undeploy Model");
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        logActivitiTask(context, LOGGER);

        info(context, Messages.BUILDING_CLOUD_UNDEPLOY_MODEL, LOGGER);
        try {
            DeployedMta deployedMta = StepsUtil.getDeployedMta(context);
            if (deployedMta == null) {
                setComponentsToUndeploy(context, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
                return ExecutionStatus.SUCCESS;
            }

            List<ConfigurationSubscription> subscriptionsToCreate = StepsUtil.getSubscriptionsToCreate(context);
            Set<String> mtaModules = StepsUtil.getMtaModules(context);
            List<CloudApplicationExtended> appsToDeploy = StepsUtil.getAppsToDeploy(context);
            List<CloudApplication> deployedApps = StepsUtil.getDeployedApps(context);

            debug(context, format(Messages.MTA_MODULES, mtaModules), LOGGER);

            List<String> appNames = appsToDeploy.stream().map((app) -> app.getName()).collect(Collectors.toList());

            List<DeployedMtaModule> modulesToUndeploy = computeModulesToUndeploy(deployedMta, mtaModules, appNames);
            List<DeployedMtaModule> modulesToKeep = computeModulesToKeep(modulesToUndeploy, deployedMta);

            List<ConfigurationSubscription> subscriptionsToDelete = computeSubscriptionsToDelete(subscriptionsToCreate, deployedMta,
                StepsUtil.getSpaceId(context));
            debug(context, format(Messages.SUBSCRIPTIONS_TO_DELETE, secureSerializer.toJson(subscriptionsToDelete)), LOGGER);

            List<String> servicesToDelete = computeServicesToDelete(deployedMta.getServices(), modulesToKeep);
            debug(context, format(Messages.SERVICES_TO_DELETE, servicesToDelete), LOGGER);

            List<CloudApplication> appsToUndeploy = computeAppsToUndeploy(modulesToUndeploy, deployedApps);
            debug(context, format(Messages.APPS_TO_UNDEPLOY, secureSerializer.toJson(appsToUndeploy)), LOGGER);

            setComponentsToUndeploy(context, servicesToDelete, appsToUndeploy, subscriptionsToDelete);

            debug(context, Messages.CLOUD_UNDEPLOY_MODEL_BUILT, LOGGER);
            return ExecutionStatus.SUCCESS;
        } catch (Exception e) {
            error(context, Messages.ERROR_BUILDING_CLOUD_UNDEPLOY_MODEL, e, LOGGER);
            throw e;
        }
    }

    private List<DeployedMtaModule> computeModulesToKeep(List<DeployedMtaModule> modulesToUndeploy, DeployedMta deployedMta) {
        return deployedMta.getModules().stream().filter((existingModule) -> modulesToUndeploy.stream().noneMatch(
            (module) -> module.getModuleName().equals(existingModule.getModuleName()))).collect(Collectors.toList());
    }

    private void setComponentsToUndeploy(DelegateExecution context, List<String> services, List<CloudApplication> apps,
        List<ConfigurationSubscription> subscriptions) {
        StepsUtil.setSubscriptionsToDelete(context, subscriptions);
        StepsUtil.setServicesToDelete(context, services);
        StepsUtil.setAppsToUndeploy(context, apps);
    }

    private List<String> computeServicesToDelete(Set<String> createdService, List<DeployedMtaModule> modulesToKeep) {
        return createdService.stream().filter(
            (service) -> modulesToKeep.stream().noneMatch((module) -> module.getServices().contains(service))).collect(Collectors.toList());
    }

    private List<DeployedMtaModule> computeModulesToUndeploy(DeployedMta deployedMta, Set<String> mtaModules, List<String> appsToDeploy) {
        return deployedMta.getModules().stream().filter((deployedModule) -> {

            if (!mtaModules.contains(deployedModule.getModuleName())) {
                return true; // Obsolete module;
            } else if (!appsToDeploy.contains(deployedModule.getAppName())) {
                return true; // Module resulting in an app with a different name; This could occur
                             // if for example namespaces for applications were used previously, but
                             // are not used now.
            } else {
                return false;
            }

        }).collect(Collectors.toList());
    }

    private List<CloudApplication> computeAppsToUndeploy(List<DeployedMtaModule> modulesToUndeploy, List<CloudApplication> deployedApps) {
        return deployedApps.stream().filter(
            (app) -> modulesToUndeploy.stream().anyMatch((module) -> module.getAppName().equals(app.getName()))).collect(
                Collectors.toList());
    }

    private List<ConfigurationSubscription> computeSubscriptionsToDelete(List<ConfigurationSubscription> subscriptionsToCreate,
        DeployedMta deployedMta, String spaceId) {
        String mtaId = deployedMta.getMetadata().getId();
        List<ConfigurationSubscription> existingSubscriptions = dao.findAll(mtaId, null, spaceId, null);
        return existingSubscriptions.stream().filter(
            (subscription) -> !willBeCreatedOrUpdated(subscription, subscriptionsToCreate)).collect(Collectors.toList());
    }

    private boolean willBeCreatedOrUpdated(ConfigurationSubscription existingSubscription,
        List<ConfigurationSubscription> createdOrUpdatedSubscriptions) {
        return createdOrUpdatedSubscriptions.stream().anyMatch((subscription) -> areEqual(subscription, existingSubscription));
    }

    protected boolean areEqual(ConfigurationSubscription subscription1, ConfigurationSubscription subscription2) {
        return Objects.equals(subscription1.getAppName(), subscription2.getAppName())
            && Objects.equals(subscription1.getSpaceId(), subscription2.getSpaceId())
            && Objects.equals(subscription1.getResourceDto().getName(), subscription2.getResourceDto().getName());
    }

}
