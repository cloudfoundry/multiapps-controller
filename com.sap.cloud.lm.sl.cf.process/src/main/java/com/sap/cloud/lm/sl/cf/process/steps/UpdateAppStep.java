package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.Staging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.activiti.common.util.ContextUtil;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.StagingExtended;
import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientFactory.PlatformType;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("updateAppStep")
public class UpdateAppStep extends CreateAppStep {

    // Logger
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateAppStep.class);

    public static StepMetadata getMetadata() {
        return new StepMetadata("updateAppTask", "Update App", "Update App");
    }

    @Override
    protected ExecutionStatus executeStep(DelegateExecution context) throws SLException, FileStorageException {
        logActivitiTask(context, LOGGER);

        // Get the next cloud application from the context
        CloudApplicationExtended app = StepsUtil.getApp(context);

        // Get the existing application from the context
        CloudApplication existingApp = StepsUtil.getExistingApp(context);

        try {
            info(context, format(Messages.UPDATING_APP, app.getName()), LOGGER);

            // Get a cloud foundry client
            CloudFoundryOperations client = clientSupplier.apply(context);

            // Get application parameters
            String appName = app.getName();
            StagingExtended staging = app.getStaging();
            Integer diskQuota = (app.getDiskQuota() != 0) ? app.getDiskQuota() : null;
            Integer memory = (app.getMemory() != 0) ? app.getMemory() : null;
            List<String> uris = app.getUris();
            Map<String, String> env = app.getEnvAsMap();

            boolean keepAppAttributes = ContextUtil.getVariable(context, Constants.PARAM_KEEP_APP_ATTRIBUTES, false);
            boolean appPropertiesChanged = false;

            // Update the application
            if (hasChanged(staging, existingApp.getStaging()) && !keepAppAttributes) {
                debug(context, format("Updating staging of application \"{0}\"", appName), LOGGER);
                if (platformTypeSupplier.get() == PlatformType.CF) {
                    applicationEntityUpdater.updateApplicationStaging(client, appName, staging);
                } else {
                    client.updateApplicationStaging(appName, staging);
                }
                appPropertiesChanged = true;
            }
            if (memory != null && !memory.equals(existingApp.getMemory()) && !keepAppAttributes) {
                debug(context, format("Updating memory of application \"{0}\"", appName), LOGGER);
                client.updateApplicationMemory(appName, memory);
                appPropertiesChanged = true;
            }
            if (diskQuota != null && !diskQuota.equals(existingApp.getDiskQuota()) && !keepAppAttributes) {
                debug(context, format("Updating disk quota of application \"{0}\"", appName), LOGGER);
                client.updateApplicationDiskQuota(appName, diskQuota);
                appPropertiesChanged = true;
            }
            if (hasChanged(uris, existingApp.getUris()) && !keepAppAttributes) {
                debug(context, format("Updating uris of application \"{0}\"", appName), LOGGER);
                client.updateApplicationUris(appName, uris);
                appPropertiesChanged = true;
            }
            if (!keepAppAttributes) {
                appPropertiesChanged = updateApplicationServices(app, existingApp, client, context) ? true : appPropertiesChanged;
            }
            if (!env.equals(existingApp.getEnvAsMap()) && !keepAppAttributes) {
                debug(context, format("Updating env of application \"{0}\"", appName), LOGGER);
                client.updateApplicationEnv(appName, env);
                appPropertiesChanged = true;
            }

            if (!appPropertiesChanged) {
                info(context, format(Messages.APPLICATION_UNCHANGED, app.getName()), LOGGER);
            } else {
                debug(context, format(Messages.APP_UPDATED, app.getName()), LOGGER);
            }

            StepsUtil.setAppPropertiesChanged(context, appPropertiesChanged);

            return ExecutionStatus.SUCCESS;
        } catch (SLException e) {
            error(context, format(Messages.ERROR_UPDATING_APP, app.getName()), e, LOGGER);
            throw e;
        } catch (CloudFoundryException e) {
            SLException ex = StepsUtil.createException(e);
            error(context, format(Messages.ERROR_UPDATING_APP, app.getName()), ex, LOGGER);
            throw ex;
        }
    }

    private boolean updateApplicationServices(CloudApplicationExtended app, CloudApplication existingApp, CloudFoundryOperations client,
        DelegateExecution context) throws SLException, FileStorageException {
        boolean hasUnboundServices = unbindNotRequiredServices(existingApp, app.getServices(), client, context);
        Map<String, Map<String, Object>> bindingParameters = getBindingParameters(context, app);
        Set<String> updatedServices = StepsUtil.getUpdatedServices(context);
        boolean hasUpdatedServices = updateServices(app, existingApp, bindingParameters, client, context, updatedServices);
        return hasUnboundServices || hasUpdatedServices;
    }

    private boolean unbindNotRequiredServices(CloudApplication app, List<String> requiredServices, CloudFoundryOperations client,
        DelegateExecution context) {
        boolean hasUnbindedService = false;
        for (String serviceName : app.getServices()) {
            if (!requiredServices.contains(serviceName)) {
                unbindService(app.getName(), serviceName, client, context);
                hasUnbindedService = true;
            }
        }
        return hasUnbindedService;
    }

    private void unbindService(String appName, String serviceName, CloudFoundryOperations client, DelegateExecution context) {
        debug(context, format(Messages.UNBINDING_APP_FROM_SERVICE, appName, serviceName), LOGGER);
        client.unbindService(appName, serviceName);
    }

    private boolean updateServices(CloudApplicationExtended app, CloudApplication existingApp,
        Map<String, Map<String, Object>> bindingParameters, CloudFoundryOperations client, DelegateExecution context,
        Set<String> updatedServices) throws SLException {
        boolean hasUpdatedService = false;
        List<String> existingAppServices = existingApp.getServices();
        for (String serviceName : app.getServices()) {
            Map<String, Object> bindingParametersForCurrentService = getBindingParametersForService(serviceName, bindingParameters);
            if (updatedServices.contains(serviceName)) {
                hasUpdatedService = true;
            }
            if (!existingAppServices.contains(serviceName)) {
                hasUpdatedService = true;
                bindService(context, client, app.getName(), serviceName, bindingParametersForCurrentService);
                continue;
            }
            List<CloudServiceBinding> existingServiceBindings = client.getServiceInstance(serviceName).getBindings();
            CloudServiceBinding existingBindingForApplication = getServiceBindingsForApplication(existingApp, existingServiceBindings);
            if (existingBindingForApplication == null) {
                hasUpdatedService = true;
                bindService(context, client, app.getName(), serviceName, bindingParametersForCurrentService);
                continue;
            }
            Map<String, Object> existingBindingParameters = existingBindingForApplication.getBindingOptions();
            if (!Objects.equals(existingBindingParameters, bindingParametersForCurrentService)) {
                unbindService(existingApp.getName(), serviceName, client, context);
                bindService(context, client, app.getName(), serviceName, bindingParametersForCurrentService);
                hasUpdatedService = true;
                continue;
            }
            info(context, format(Messages.WILL_NOT_REBIND_APP_TO_SERVICE, serviceName, app.getName()), LOGGER);
        }
        return hasUpdatedService;
    }

    protected CloudServiceBinding getServiceBindingsForApplication(CloudApplication existingApp,
        List<CloudServiceBinding> serviceBindings) {
        Optional<CloudServiceBinding> optCloudServiceBinding = serviceBindings.stream().filter(
            serviceBinding -> existingApp.getMeta().getGuid().equals(serviceBinding.getAppGuid())).findFirst();
        if (optCloudServiceBinding.isPresent()) {
            return optCloudServiceBinding.get();
        }
        return null;
    }

    private boolean hasChanged(StagingExtended staging, Staging existingStaging) {
        String buildpackUrl = staging.getBuildpackUrl();
        String command = staging.getCommand();
        String stack = staging.getStack();
        Integer healthCheckTimeout = staging.getHealthCheckTimeout();
        String healthCheckType = staging.getHealthCheckType();
        return (buildpackUrl != null && !buildpackUrl.equals(existingStaging.getBuildpackUrl()))
            || (command != null && !command.equals(existingStaging.getCommand()))
            || (stack != null && !stack.equals(existingStaging.getStack()))
            || (healthCheckTimeout != null && !healthCheckTimeout.equals(existingStaging.getHealthCheckTimeout()))
            || (healthCheckType != null);
    }

    private boolean hasChanged(List<String> uris, List<String> existingUris) {
        if (uris.size() != existingUris.size())
            return true;
        for (int i = 0; i < uris.size(); i++) {
            String uri = uris.get(i);
            String existingUri = existingUris.get(i);
            if (!uri.equals(existingUri) && !uri.equals(removeSchema(existingUri))) {
                return true;
            }
        }
        return false;
    }

    private static String removeSchema(String uri) {
        return uri.substring(uri.indexOf("://") + 3);
    }

}
