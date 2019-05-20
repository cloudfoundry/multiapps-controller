package com.sap.cloud.lm.sl.cf.process.steps;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.SetUtils;
import org.cloudfoundry.client.lib.ApplicationServicesUpdateCallback;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended.AttributeUpdateStrategy;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ServiceKeyToInject;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaArchiveElements;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.persistence.processors.DefaultFileDownloadProcessor;
import com.sap.cloud.lm.sl.cf.persistence.services.FileContentProcessor;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ApplicationAttributeUpdater;
import com.sap.cloud.lm.sl.cf.process.util.ApplicationAttributeUpdater.UpdateState;
import com.sap.cloud.lm.sl.cf.process.util.DiskQuotaApplicationAttributeUpdater;
import com.sap.cloud.lm.sl.cf.process.util.ElementUpdater.UpdateBehavior;
import com.sap.cloud.lm.sl.cf.process.util.EnvironmentApplicationAttributeUpdater;
import com.sap.cloud.lm.sl.cf.process.util.MemoryApplicationAttributeUpdater;
import com.sap.cloud.lm.sl.cf.process.util.ServiceOperationUtil;
import com.sap.cloud.lm.sl.cf.process.util.StagingApplicationAttributeUpdater;
import com.sap.cloud.lm.sl.cf.process.util.UrisApplicationAttributeUpdater;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.mta.handlers.ArchiveHandler;
import com.sap.cloud.lm.sl.mta.util.ValidatorUtil;

@Component("createOrUpdateAppStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CreateOrUpdateAppStep extends SyncFlowableStep {

    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Autowired
    protected ApplicationConfiguration configuration;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws FileStorageException {
        CloudApplicationExtended app = StepsUtil.getApp(execution.getContext());

        try {
            CloudControllerClient client = execution.getControllerClient();
            CloudApplication existingApp = client.getApplication(app.getName(), false);
            StepsUtil.setExistingApp(execution.getContext(), existingApp);

            StepFlowHandler flowHandler = createStepFlowHandler(execution, client, app, existingApp);

            flowHandler.printStepStartMessage();

            flowHandler.handleApplicationAttributes();
            flowHandler.injectServiceKeysCredentialsInAppEnv();
            flowHandler.handleApplicationServices();
            flowHandler.handleApplicationEnv();

            flowHandler.printStepEndMessage();

            return StepPhase.DONE;
        } catch (CloudOperationException coe) {
            CloudControllerException e = new CloudControllerException(coe);
            getStepLogger().error(e, Messages.ERROR_CREATING_OR_UPDATING_APP, app.getName());
            throw e;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_CREATING_OR_UPDATING_APP, app.getName());
            throw e;
        }
    }

    private StepFlowHandler createStepFlowHandler(ExecutionWrapper execution, CloudControllerClient client, CloudApplicationExtended app,
        CloudApplication existingApp) {
        if (existingApp == null) {
            return new CreateAppFlowHandler(execution, client, app);
        }
        return new UpdateAppFlowHandler(execution, client, app, existingApp);
    }

    private abstract class StepFlowHandler {

        ExecutionWrapper execution;
        CloudApplicationExtended app;
        CloudControllerClient client;

        public StepFlowHandler(ExecutionWrapper execution, CloudControllerClient client, CloudApplicationExtended app) {
            this.execution = execution;
            this.app = app;
            this.client = client;
        }

        public void injectServiceKeysCredentialsInAppEnv() {
            Map<String, String> appEnv = app.getEnvAsMap();
            Map<String, String> appServiceKeysCredentials = buildServiceKeysCredentials(client, app, appEnv);
            app.setEnv(MapUtil.upcast(appEnv));
            updateContextWithServiceKeysCredentials(execution.getContext(), app, appServiceKeysCredentials);
        }

        public abstract void printStepStartMessage();

        public abstract void handleApplicationAttributes();

        public abstract void handleApplicationServices() throws FileStorageException;

        public abstract void handleApplicationEnv();

        public abstract void printStepEndMessage();
    }

    private class CreateAppFlowHandler extends StepFlowHandler {

        public CreateAppFlowHandler(ExecutionWrapper execution, CloudControllerClient client, CloudApplicationExtended app) {
            super(execution, client, app);
        }

        @Override
        public void handleApplicationAttributes() {
            Integer diskQuota = (app.getDiskQuota() != 0) ? app.getDiskQuota() : null;
            Integer memory = (app.getMemory() != 0) ? app.getMemory() : null;
            List<String> uris = app.getUris();

            if (app.getDockerInfo() != null) {
                execution.getStepLogger()
                    .info(Messages.CREATING_APP_FROM_DOCKER_IMAGE, app.getName(), app.getDockerInfo()
                        .getImage());
            }
            client.createApplication(app.getName(), app.getStaging(), diskQuota, memory, uris, Collections.emptyList(),
                app.getDockerInfo());
            StepsUtil.setVcapAppPropertiesChanged(execution.getContext(), true);
        }

        @Override
        public void handleApplicationServices() throws FileStorageException {
            List<String> services = app.getServices();
            Map<String, Map<String, Object>> bindingParameters = getBindingParameters(execution.getContext(), app);
            for (String serviceName : services) {
                Map<String, Object> bindingParametersForCurrentService = getBindingParametersForService(serviceName, bindingParameters);
                bindService(execution, client, app.getName(), serviceName, bindingParametersForCurrentService);
            }
            StepsUtil.setVcapServicesPropertiesChanged(execution.getContext(), true);
        }

        @Override
        public void handleApplicationEnv() {
            client.updateApplicationEnv(app.getName(), app.getEnvAsMap());
            StepsUtil.setUserPropertiesChanged(execution.getContext(), true);
        }

        @Override
        public void printStepStartMessage() {
            getStepLogger().info(Messages.CREATING_APP_FROM_MODULE, app.getName(), app.getModuleName());
        }

        @Override
        public void printStepEndMessage() {
            getStepLogger().debug(Messages.APP_CREATED, app.getName());
        }

    }

    private class UpdateAppFlowHandler extends StepFlowHandler {

        CloudApplication existingApp;

        public UpdateAppFlowHandler(ExecutionWrapper execution, CloudControllerClient client, CloudApplicationExtended app,
            CloudApplication existingApp) {
            super(execution, client, app);
            this.existingApp = existingApp;
        }

        @Override
        public void handleApplicationAttributes() {
            List<UpdateState> updateStates = getApplicationAttributeUpdaters(existingApp, app.getApplicationAttributesUpdateStrategy())
                .stream()
                .map(updater -> updater.updateApplication(client, app))
                .collect(Collectors.toList());

            boolean arePropertiesChanged = updateStates.stream()
                .anyMatch(updateState -> updateState == UpdateState.UPDATED);

            reportApplicationUpdateStatus(app, arePropertiesChanged);
            StepsUtil.setVcapAppPropertiesChanged(execution.getContext(), arePropertiesChanged);

        }

        @Override
        public void handleApplicationServices() throws FileStorageException {
            List<String> services = app.getServices();
            boolean hasUnboundServices = unbindServicesIfNeeded(app, existingApp, client, services);

            Map<String, Map<String, Object>> bindingParameters = getBindingParameters(execution.getContext(), app);

            boolean hasUpdatedServices = updateServices(execution.getContext(), app.getName(), bindingParameters, client,
                calculateServicesForUpdate(app, existingApp.getServices()));

            StepsUtil.setVcapServicesPropertiesChanged(execution.getContext(), hasUnboundServices || hasUpdatedServices);
        }

        @Override
        public void handleApplicationEnv() {
            Map<String, String> envAsMap = app.getEnvAsMap();
            updateAppDigest(envAsMap, existingApp.getEnvAsMap());
            app.setEnv(MapUtil.unmodifiable(envAsMap));

            UpdateState updateApplicationEnvironmentState = updateApplicationEnvironment(app, existingApp, client,
                app.getApplicationAttributesUpdateStrategy());

            StepsUtil.setUserPropertiesChanged(execution.getContext(), updateApplicationEnvironmentState == UpdateState.UPDATED);
        }

        @Override
        public void printStepStartMessage() {
            getStepLogger().info(Messages.UPDATING_APP, app.getName());
        }

        @Override
        public void printStepEndMessage() {
            getStepLogger().debug(Messages.APP_UPDATED, app.getName());
        }

    }

    private UpdateState updateApplicationEnvironment(CloudApplicationExtended app, CloudApplication existingApp,
        CloudControllerClient client, CloudApplicationExtended.AttributeUpdateStrategy applicationAttributesUpdateStrategy) {
        return new EnvironmentApplicationAttributeUpdater(existingApp,
            getUpdateStrategy(applicationAttributesUpdateStrategy.shouldKeepExistingEnv()), getStepLogger()).updateApplication(client, app);
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

    private Set<String> calculateServicesForUpdate(CloudApplicationExtended application, List<String> existingServices) {
        AttributeUpdateStrategy applicationAttributesUpdateBehavior = application.getApplicationAttributesUpdateStrategy();
        if (!applicationAttributesUpdateBehavior.shouldKeepExistingServiceBindings()) {
            return new HashSet<>(application.getServices());
        }

        return SetUtils.union(new HashSet<>(application.getServices()), new HashSet<>(existingServices));
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
        servicesToUnbind.forEach(serviceName -> unbindService(app.getName(), serviceName, client));
        return !servicesToUnbind.isEmpty();
    }

    private void unbindService(String appName, String serviceName, CloudControllerClient client) {
        getStepLogger().debug(Messages.UNBINDING_APP_FROM_SERVICE, appName, serviceName);
        client.unbindService(appName, serviceName);
    }

    private boolean updateServices(DelegateExecution context, String applicationName, Map<String, Map<String, Object>> bindingParameters,
        CloudControllerClient client, Set<String> services) {
        Map<String, Map<String, Object>> serviceNamesWithBindingParameters = services.stream()
            .collect(Collectors.toMap(String::toString, serviceName -> getBindingParametersForService(serviceName, bindingParameters)));

        List<String> updatedServices = client.updateApplicationServices(applicationName, serviceNamesWithBindingParameters,
            getApplicationServicesUpdateCallback(context));

        reportNonUpdatedServices(services, applicationName, updatedServices);
        return !updatedServices.isEmpty();
    }

    private void reportNonUpdatedServices(Set<String> services, String applicationName, List<String> updatedServices) {
        List<String> nonUpdatesServices = ListUtils.removeAll(services, updatedServices);
        nonUpdatesServices.forEach(service -> getStepLogger().warn(Messages.WILL_NOT_REBIND_APP_TO_SERVICE, service, applicationName));
    }

    private Map<String, String> buildServiceKeysCredentials(CloudControllerClient client, CloudApplicationExtended app,
        Map<String, String> appEnv) {
        Map<String, String> appServiceKeysCredentials = new HashMap<>();
        for (ServiceKeyToInject serviceKeyToInject : app.getServiceKeysToInject()) {
            String serviceKeyCredentials = JsonUtil.toJson(ServiceOperationUtil.getServiceKeyCredentials(client,
                serviceKeyToInject.getServiceName(), serviceKeyToInject.getServiceKeyName()), true);
            appEnv.put(serviceKeyToInject.getEnvVarName(), serviceKeyCredentials);
            appServiceKeysCredentials.put(serviceKeyToInject.getEnvVarName(), serviceKeyCredentials);
        }
        return appServiceKeysCredentials;
    }

    private void updateContextWithServiceKeysCredentials(DelegateExecution context, CloudApplicationExtended app,
        Map<String, String> appServiceKeysCredentials) {
        Map<String, Map<String, String>> serviceKeysCredentialsToInject = StepsUtil.getServiceKeysCredentialsToInject(context);
        serviceKeysCredentialsToInject.put(app.getName(), appServiceKeysCredentials);

        // Update current process context
        StepsUtil.setApp(context, app);
        StepsUtil.setServiceKeysCredentialsToInject(context, serviceKeysCredentialsToInject);
    }

    private Map<String, Map<String, Object>> getBindingParameters(DelegateExecution context, CloudApplicationExtended app)
        throws FileStorageException {
        List<CloudServiceExtended> services = getServices(StepsUtil.getServicesToBind(context), app.getServices());

        Map<String, Map<String, Object>> descriptorProvidedBindingParameters = app.getBindingParameters();
        if (descriptorProvidedBindingParameters == null) {
            descriptorProvidedBindingParameters = Collections.emptyMap();
        }
        Map<String, Map<String, Object>> fileProvidedBindingParameters = getFileProvidedBindingParameters(context, app.getModuleName(),
            services);
        Map<String, Map<String, Object>> bindingParameters = mergeBindingParameters(descriptorProvidedBindingParameters,
            fileProvidedBindingParameters);
        getStepLogger().debug(Messages.BINDING_PARAMETERS_FOR_APPLICATION, app.getName(), secureSerializer.toJson(bindingParameters));
        return bindingParameters;
    }

    private static List<CloudServiceExtended> getServices(List<CloudServiceExtended> services, List<String> serviceNames) {
        return services.stream()
            .filter(service -> serviceNames.contains(service.getName()))
            .collect(Collectors.toList());
    }

    private Map<String, Map<String, Object>> getFileProvidedBindingParameters(DelegateExecution context, String moduleName,
        List<CloudServiceExtended> services) throws FileStorageException {
        Map<String, Map<String, Object>> result = new TreeMap<>();
        for (CloudServiceExtended service : services) {
            String requiredDependencyName = ValidatorUtil.getPrefixedName(moduleName, service.getResourceName(),
                com.sap.cloud.lm.sl.cf.core.Constants.MTA_ELEMENT_SEPARATOR);
            addFileProvidedBindingParameters(context, service.getName(), requiredDependencyName, result);
        }
        return result;
    }

    private void addFileProvidedBindingParameters(DelegateExecution context, String serviceName, String requiredDependencyName,
        Map<String, Map<String, Object>> result) throws FileStorageException {
        String archiveId = StepsUtil.getRequiredString(context, Constants.PARAM_APP_ARCHIVE_ID);
        MtaArchiveElements mtaArchiveElements = StepsUtil.getMtaArchiveElements(context);
        String fileName = mtaArchiveElements.getRequiredDependencyFileName(requiredDependencyName);
        if (fileName == null) {
            return;
        }
        FileContentProcessor fileProcessor = archive -> {
            try (InputStream file = ArchiveHandler.getInputStream(archive, fileName, configuration.getMaxManifestSize())) {
                MapUtil.addNonNull(result, serviceName, JsonUtil.convertJsonToMap(file));
            } catch (IOException e) {
                throw new SLException(e, Messages.ERROR_RETRIEVING_MTA_REQUIRED_DEPENDENCY_CONTENT, fileName);
            }
        };
        fileService.processFileContent(new DefaultFileDownloadProcessor(StepsUtil.getSpaceId(context), archiveId, fileProcessor));
    }

    private static Map<String, Map<String, Object>> mergeBindingParameters(
        Map<String, Map<String, Object>> descriptorProvidedBindingParameters,
        Map<String, Map<String, Object>> fileProvidedBindingParameters) {
        Map<String, Map<String, Object>> bindingParameters = new HashMap<>();
        Set<String> serviceNames = new HashSet<>(descriptorProvidedBindingParameters.keySet());
        serviceNames.addAll(fileProvidedBindingParameters.keySet());
        for (String serviceName : serviceNames) {
            bindingParameters.put(serviceName,
                MapUtil.mergeSafely(fileProvidedBindingParameters.get(serviceName), descriptorProvidedBindingParameters.get(serviceName)));
        }
        return bindingParameters;
    }

    private void bindService(ExecutionWrapper execution, CloudControllerClient client, String appName, String serviceName,
        Map<String, Object> bindingParameters) {

        getStepLogger().debug(Messages.BINDING_APP_TO_SERVICE_WITH_PARAMETERS, appName, serviceName, bindingParameters);
        client.bindService(appName, serviceName, bindingParameters, getApplicationServicesUpdateCallback(execution.getContext()));
    }

    private static Map<String, Object> getBindingParametersForService(String serviceName,
        Map<String, Map<String, Object>> bindingParameters) {
        return bindingParameters == null ? Collections.emptyMap() : bindingParameters.getOrDefault(serviceName, Collections.emptyMap());
    }

    protected ApplicationServicesUpdateCallback getApplicationServicesUpdateCallback(DelegateExecution context) {
        return new DefaultApplicationServicesUpdateCallback(context);
    }

    private class DefaultApplicationServicesUpdateCallback implements ApplicationServicesUpdateCallback {

        private DelegateExecution context;

        private DefaultApplicationServicesUpdateCallback(DelegateExecution context) {
            this.context = context;
        }

        @Override
        public void onError(CloudOperationException e, String applicationName, String serviceName) {
            List<CloudServiceExtended> servicesToBind = StepsUtil.getServicesToBind(context);
            CloudServiceExtended serviceToBind = findServiceCloudModel(servicesToBind, serviceName);

            if (serviceToBind != null && serviceToBind.isOptional()) {
                getStepLogger().warn(e, Messages.COULD_NOT_BIND_APP_TO_OPTIONAL_SERVICE, applicationName, serviceName);
                return;
            }
            throw new SLException(e, Messages.COULD_NOT_BIND_APP_TO_SERVICE, applicationName, serviceName, e.getMessage());
        }

        private CloudServiceExtended findServiceCloudModel(List<CloudServiceExtended> servicesCloudModel, String serviceName) {
            return servicesCloudModel.stream()
                .filter(service -> service.getName()
                    .equals(serviceName))
                .findAny()
                .orElse(null);
        }

    }
}
