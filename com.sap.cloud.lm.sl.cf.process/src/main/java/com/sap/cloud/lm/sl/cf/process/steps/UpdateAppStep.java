package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended.AttributeUpdateStrategy;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ApplicationAttributeUpdater;
import com.sap.cloud.lm.sl.cf.process.util.ApplicationAttributeUpdater.UpdateState;
import com.sap.cloud.lm.sl.cf.process.util.DiskQuotaApplicationAttributeUpdater;
import com.sap.cloud.lm.sl.cf.process.util.ElementUpdater.UpdateBehavior;
import com.sap.cloud.lm.sl.cf.process.util.EnvironmentApplicationAttributeUpdater;
import com.sap.cloud.lm.sl.cf.process.util.MemoryApplicationAttributeUpdater;
import com.sap.cloud.lm.sl.cf.process.util.StagingApplicationAttributeUpdater;
import com.sap.cloud.lm.sl.cf.process.util.UrisApplicationAttributeUpdater;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Component("updateAppStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateAppStep extends CreateAppStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws FileStorageException {

        CloudApplicationExtended app = StepsUtil.getApp(execution.getContext());

        CloudApplication existingApp = StepsUtil.getExistingApp(execution.getContext());

        try {
            getStepLogger().info(Messages.UPDATING_APP, app.getName());

            CloudControllerClient client = execution.getControllerClient();

            CloudApplicationExtended.AttributeUpdateStrategy applicationAttributesUpdateBehavior = app
                .getApplicationAttributesUpdateStrategy();
            List<UpdateState> applicationAttributesUpdateStates = updateApplicationAttributes(app, existingApp, client,
                applicationAttributesUpdateBehavior);

            boolean appPropertiesChanged = applicationAttributesUpdateStates.stream()
                .anyMatch(updateState -> updateState == UpdateState.UPDATED);

            UpdateState updateApplicationEnvironmentState = updateApplicationEnvironment(app, existingApp, client,
                applicationAttributesUpdateBehavior);
            boolean userPropertiesChanged = updateApplicationEnvironmentState == UpdateState.UPDATED;

            boolean servicesPropertiesChanged = updateApplicationServices(app, existingApp, client, execution);

            Map<String, String> env = app.getEnvAsMap();
            injectServiceKeysCredentialsInAppEnv(execution.getContext(), client, app, env);
            updateAppDigest(env, existingApp.getEnvAsMap());

            reportApplicationUpdateStatus(app, appPropertiesChanged);

            StepsUtil.setVcapAppPropertiesChanged(execution.getContext(), appPropertiesChanged);
            StepsUtil.setVcapServicesPropertiesChanged(execution.getContext(), servicesPropertiesChanged);
            StepsUtil.setUserPropertiesChanged(execution.getContext(), userPropertiesChanged);
            return StepPhase.DONE;
        } catch (CloudOperationException coe) {
            CloudControllerException e = new CloudControllerException(coe);
            getStepLogger().error(e, Messages.ERROR_UPDATING_APP, app.getName());
            throw e;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_UPDATING_APP, app.getName());
            throw e;
        }
    }

    private UpdateState updateApplicationEnvironment(CloudApplicationExtended app, CloudApplication existingApp,
        CloudControllerClient client, CloudApplicationExtended.AttributeUpdateStrategy applicationAttributesUpdateBehavior) {
        return new EnvironmentApplicationAttributeUpdater(existingApp,
            getUpdateStrategy(applicationAttributesUpdateBehavior.shouldKeepExistingEnv()), getStepLogger()).updateApplication(client, app);
    }

    private List<UpdateState> updateApplicationAttributes(CloudApplicationExtended app, CloudApplication existingApp,
        CloudControllerClient client, CloudApplicationExtended.AttributeUpdateStrategy applicationAttributesUpdateBehavior) {
        return getApplicationAttributeUpdaters(existingApp, applicationAttributesUpdateBehavior).stream()
            .map(updater -> updater.updateApplication(client, app))
            .collect(Collectors.toList());
    }

    private void reportApplicationUpdateStatus(CloudApplicationExtended app, boolean appPropertiesChanged) {
        if (!appPropertiesChanged) {
            getStepLogger().info(Messages.APPLICATION_UNCHANGED, app.getName());
            return;
        }
        getStepLogger().debug(Messages.APP_UPDATED, app.getName());
    }

    private List<ApplicationAttributeUpdater> getApplicationAttributeUpdaters(CloudApplication existingApplication,
        AttributeUpdateStrategy applicationAttributesUpdateStrategy) {
        return Arrays.asList(new StagingApplicationAttributeUpdater(existingApplication, getStepLogger()),
            new MemoryApplicationAttributeUpdater(existingApplication, getStepLogger()),
            new DiskQuotaApplicationAttributeUpdater(existingApplication, getStepLogger()), new UrisApplicationAttributeUpdater(
                existingApplication, getUpdateStrategy(applicationAttributesUpdateStrategy.shouldKeepExistingRoutes()), getStepLogger()));
    }

    private UpdateBehavior getUpdateStrategy(boolean shouldKeepAttributes) {
        return shouldKeepAttributes ? UpdateBehavior.MERGE : UpdateBehavior.REPLACE;
    }

    private void updateAppDigest(Map<String, String> newAppEnv, Map<String, String> existingAppEnv) {
        Object existingFileDigest = getExistingAppFileDigest(existingAppEnv);
        if (existingFileDigest == null) {
            return;
        }
        String newAppDeployAttributes = newAppEnv.get(com.sap.cloud.lm.sl.cf.core.Constants.ENV_DEPLOY_ATTRIBUTES);
        TreeMap<String, Object> newAppDeployAttributesMap = new TreeMap<>(JsonUtil.convertJsonToMap(newAppDeployAttributes));
        newAppDeployAttributesMap.put(com.sap.cloud.lm.sl.cf.core.Constants.ATTR_APP_CONTENT_DIGEST, existingFileDigest);
        newAppEnv.put(com.sap.cloud.lm.sl.cf.core.Constants.ENV_DEPLOY_ATTRIBUTES, JsonUtil.toJson(newAppDeployAttributesMap, true));
    }

    private Object getExistingAppFileDigest(Map<String, String> envAsMap) {
        String applicationDeployAttributes = envAsMap.get(com.sap.cloud.lm.sl.cf.core.Constants.ENV_DEPLOY_ATTRIBUTES);
        Map<String, Object> deployAttributesMap = JsonUtil.convertJsonToMap(applicationDeployAttributes);
        return deployAttributesMap.get(com.sap.cloud.lm.sl.cf.core.Constants.ATTR_APP_CONTENT_DIGEST);
    }

    private boolean updateApplicationServices(CloudApplicationExtended application, CloudApplication existingApplication,
        CloudControllerClient client, ExecutionWrapper execution) throws FileStorageException {
        List<String> services = application.getServices();
        boolean hasUnboundServices = unbindServicesIfNeeded(application, existingApplication, client, services);

        Map<String, Map<String, Object>> bindingParameters = getBindingParameters(execution.getContext(), application);

        boolean hasUpdatedServices = updateServices(execution.getContext(), application.getName(), bindingParameters, client,
            calculateServicesForUpdate(application, existingApplication.getServices()));

        return hasUnboundServices || hasUpdatedServices;
    }

    private List<String> calculateServicesForUpdate(CloudApplicationExtended application, List<String> existingServices) {
        AttributeUpdateStrategy applicationAttributesUpdateBehavior = application.getApplicationAttributesUpdateStrategy();
        if (!applicationAttributesUpdateBehavior.shouldKeepExistingServiceBindings()) {
            return application.getServices();
        }

        return ListUtils.union(application.getServices(), existingServices);
    }

    private boolean unbindServicesIfNeeded(CloudApplicationExtended application, CloudApplication existingApplication,
        CloudControllerClient client, List<String> services) {
        AttributeUpdateStrategy applicationAttributesUpdateBehavior = application.getApplicationAttributesUpdateStrategy();
        if (!applicationAttributesUpdateBehavior.shouldKeepExistingServiceBindings()) {
            return unbindNotRequiredServices(existingApplication, services, client);
        }
        return false;
    }

    private boolean unbindNotRequiredServices(CloudApplication app, List<String> requiredServices, CloudControllerClient client) {
        List<String> servicesToUnbind = app.getServices()
            .stream()
            .filter(serviceName -> !requiredServices.contains(serviceName))
            .collect(Collectors.toList());
        servicesToUnbind.forEach((serviceName) -> unbindService(app.getName(), serviceName, client));
        return !servicesToUnbind.isEmpty();
    }

    private void unbindService(String appName, String serviceName, CloudControllerClient client) {
        getStepLogger().debug(Messages.UNBINDING_APP_FROM_SERVICE, appName, serviceName);
        client.unbindService(appName, serviceName);
    }

    private boolean updateServices(DelegateExecution context, String applicationName, Map<String, Map<String, Object>> bindingParameters,
        CloudControllerClient client, List<String> services) {
        Map<String, Map<String, Object>> serviceNamesWithBindingParameters = services.stream()
            .collect(Collectors.toMap(String::toString, serviceName -> getBindingParametersForService(serviceName, bindingParameters)));

        List<String> updatedServices = client.updateApplicationServices(applicationName, serviceNamesWithBindingParameters,
            getApplicationServicesUpdateCallback(context));

        reportNonUpdatedServices(services, updatedServices);
        return !updatedServices.isEmpty();
    }

    private void reportNonUpdatedServices(List<String> services, List<String> updatedServices) {
        List<String> nonUpdatesServices = ListUtils.removeAll(services, updatedServices);
        nonUpdatesServices.forEach(service -> getStepLogger().warn(Messages.WILL_NOT_REBIND_APP_TO_SERVICE, service));
    }

}
