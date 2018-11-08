package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.DockerInfo;
import org.cloudfoundry.client.lib.domain.Staging;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.cf.PlatformType;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Component("updateAppStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateAppStep extends CreateAppStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws FileStorageException {
        // Get the next cloud application from the context
        CloudApplicationExtended app = StepsUtil.getApp(execution.getContext());

        // Get the existing application from the context
        CloudApplication existingApp = StepsUtil.getExistingApp(execution.getContext());

        try {
            getStepLogger().info(Messages.UPDATING_APP, app.getName());

            // Get a cloud foundry client
            CloudControllerClient client = execution.getControllerClient();

            // Get application parameters
            String appName = app.getName();
            Staging staging = app.getStaging();
            Integer diskQuota = (app.getDiskQuota() != 0) ? app.getDiskQuota() : null;
            Integer memory = (app.getMemory() != 0) ? app.getMemory() : null;
            List<String> uris = app.getUris();
            Map<String, String> env = app.getEnvAsMap();

            boolean appPropertiesChanged = false;
            boolean servicesPropertiesChanged = false;
            boolean userPropertiesChanged = false;

            // Update the application
            if (hasChanged(staging, existingApp.getStaging())) {
                getStepLogger().debug("Updating staging of application \"{0}\"", appName);
                if (configuration.getPlatformType() == PlatformType.CF) {
                    applicationStagingUpdater.updateApplicationStaging(client, appName, staging);
                } else {
                    client.updateApplicationStaging(appName, staging);
                }
                appPropertiesChanged = true;
            }
            if (memory != null && !memory.equals(existingApp.getMemory())) {
                getStepLogger().debug("Updating memory of application \"{0}\"", appName);
                client.updateApplicationMemory(appName, memory);
                appPropertiesChanged = true;
            }
            if (diskQuota != null && !diskQuota.equals(existingApp.getDiskQuota())) {
                getStepLogger().debug("Updating disk quota of application \"{0}\"", appName);
                client.updateApplicationDiskQuota(appName, diskQuota);
                appPropertiesChanged = true;
            }
            if (hasChanged(uris, existingApp.getUris())) {
                getStepLogger().debug("Updating uris of application \"{0}\" with uri: {1}", appName, uris);
                client.updateApplicationUris(appName, uris);
                appPropertiesChanged = true;
            }
            servicesPropertiesChanged = updateApplicationServices(app, existingApp, client, execution);
            injectServiceKeysCredentialsInAppEnv(execution.getContext(), client, app, env);
            updateAppDigest(env, existingApp.getEnvAsMap());
            if (!env.equals(existingApp.getEnvAsMap())) {
                getStepLogger().debug("Updating env of application \"{0}\"", appName);
                getStepLogger().debug("Updated env: {0}", JsonUtil.toJson(env, true));
                client.updateApplicationEnv(appName, env);
                userPropertiesChanged = true;
            }

            if (!appPropertiesChanged) {
                getStepLogger().info(Messages.APPLICATION_UNCHANGED, app.getName());
            } else {
                getStepLogger().debug(Messages.APP_UPDATED, app.getName());
            }

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

    private boolean updateApplicationServices(CloudApplicationExtended app, CloudApplication existingApp, CloudControllerClient client,
        ExecutionWrapper execution) throws FileStorageException {
        boolean hasUnboundServices = unbindNotRequiredServices(existingApp, app.getServices(), client);
        List<String> services = app.getServices();
        Map<String, Map<String, Object>> bindingParameters = getBindingParameters(execution.getContext(), app);
        boolean hasUpdatedServices = updateServices(app, existingApp, bindingParameters, client, execution, services);
        return hasUnboundServices || hasUpdatedServices;
    }

    private boolean unbindNotRequiredServices(CloudApplication app, List<String> requiredServices, CloudControllerClient client) {
        boolean hasUnbindedService = false;
        for (String serviceName : app.getServices()) {
            if (!requiredServices.contains(serviceName)) {
                unbindService(app.getName(), serviceName, client);
                hasUnbindedService = true;
            }
        }
        return hasUnbindedService;
    }

    private void unbindService(String appName, String serviceName, CloudControllerClient client) {
        getStepLogger().debug(Messages.UNBINDING_APP_FROM_SERVICE, appName, serviceName);
        client.unbindService(appName, serviceName);
    }

    private boolean updateServices(CloudApplicationExtended app, CloudApplication existingApp,
        Map<String, Map<String, Object>> bindingParameters, CloudControllerClient client, ExecutionWrapper execution, List<String> services) {
        boolean hasUpdatedService = false;
        List<String> existingAppServices = existingApp.getServices();
        for (String serviceName : services) {
            Map<String, Object> bindingParametersForCurrentService = getBindingParametersForService(serviceName, bindingParameters);
            if (isServiceUpdated(serviceName, execution)) {
                hasUpdatedService = true;
            }
            if (!existingAppServices.contains(serviceName)) {
                hasUpdatedService = true;
                bindService(execution, client, app.getName(), serviceName, bindingParametersForCurrentService);
                continue;
            }
            List<CloudServiceBinding> existingServiceBindings = client.getServiceInstance(serviceName)
                .getBindings();
            CloudServiceBinding existingBindingForApplication = getServiceBindingsForApplication(existingApp, existingServiceBindings);
            if (existingBindingForApplication == null) {
                hasUpdatedService = true;
                bindService(execution, client, app.getName(), serviceName, bindingParametersForCurrentService);
                continue;
            }
            Map<String, Object> existingBindingParameters = getBindingParametersOrDefault(existingBindingForApplication);
            if (!Objects.equals(existingBindingParameters, bindingParametersForCurrentService)) {
                unbindService(existingApp.getName(), serviceName, client);
                bindService(execution, client, app.getName(), serviceName, bindingParametersForCurrentService);
                hasUpdatedService = true;
                continue;
            }
            getStepLogger().info(Messages.WILL_NOT_REBIND_APP_TO_SERVICE, serviceName, app.getName());
        }
        return hasUpdatedService;
    }

    private boolean isServiceUpdated(String serviceName, ExecutionWrapper execution) {
        return StepsUtil.getIsServiceUpdatedExportedVariable(serviceName, execution.getContext());
    }

    protected CloudServiceBinding getServiceBindingsForApplication(CloudApplication existingApp,
        List<CloudServiceBinding> serviceBindings) {
        Optional<CloudServiceBinding> optCloudServiceBinding = serviceBindings.stream()
            .filter(serviceBinding -> existingApp.getMeta()
                .getGuid()
                .equals(serviceBinding.getAppGuid()))
            .findFirst();
        if (optCloudServiceBinding.isPresent()) {
            return optCloudServiceBinding.get();
        }
        return null;
    }

    private boolean hasChanged(Staging staging, Staging existingStaging) {
        String buildpackUrl = staging.getBuildpackUrl();
        String command = staging.getCommand();
        String stack = staging.getStack();
        Integer healthCheckTimeout = staging.getHealthCheckTimeout();
        String healthCheckType = staging.getHealthCheckType();
        String healthCheckHttpEndpoint = staging.getHealthCheckHttpEndpoint();
        Boolean sshEnabled = staging.isSshEnabled();
        return (buildpackUrl != null && !buildpackUrl.equals(existingStaging.getBuildpackUrl()))
            || (command != null && !command.equals(existingStaging.getCommand()))
            || (stack != null && !stack.equals(existingStaging.getStack()))
            || (healthCheckTimeout != null && !healthCheckTimeout.equals(existingStaging.getHealthCheckTimeout()))
            || (healthCheckType != null && !healthCheckType.equals(existingStaging.getHealthCheckType()))
            || (healthCheckHttpEndpoint != null && !healthCheckHttpEndpoint.equals(existingStaging.getHealthCheckHttpEndpoint()))
            || (sshEnabled != null && !sshEnabled.equals(existingStaging.isSshEnabled())
                || isDockerInfoModified(existingStaging.getDockerInfo(), staging.getDockerInfo()));
    }

    private boolean isDockerInfoModified(DockerInfo existingDockerInfo, DockerInfo newDockerInfo) {

        return existingDockerInfo != null && newDockerInfo != null && !existingDockerInfo.getImage()
            .equals(newDockerInfo.getImage());
    }

    private boolean hasChanged(List<String> uris, List<String> existingUris) {
        Set<String> urisSet = new HashSet<>(uris);
        Set<String> existingUrisSet = new HashSet<>(existingUris);
        return !urisSet.equals(existingUrisSet);
    }

}
