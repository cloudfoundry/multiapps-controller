package org.cloudfoundry.multiapps.controller.process.steps;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.common.util.MapUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended.AttributeUpdateStrategy;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ServiceKeyToInject;
import org.cloudfoundry.multiapps.controller.core.helpers.ApplicationFileDigestDetector;
import org.cloudfoundry.multiapps.controller.core.helpers.MtaArchiveElements;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerialization;
import org.cloudfoundry.multiapps.controller.persistence.services.FileContentProcessor;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ApplicationAttributeUpdater;
import org.cloudfoundry.multiapps.controller.process.util.ApplicationAttributeUpdater.UpdateState;
import org.cloudfoundry.multiapps.controller.process.util.ApplicationServicesUpdater;
import org.cloudfoundry.multiapps.controller.process.util.ControllerClientFacade;
import org.cloudfoundry.multiapps.controller.process.util.DiskQuotaApplicationAttributeUpdater;
import org.cloudfoundry.multiapps.controller.process.util.ElementUpdater.UpdateStrategy;
import org.cloudfoundry.multiapps.controller.process.util.EnvironmentApplicationAttributeUpdater;
import org.cloudfoundry.multiapps.controller.process.util.MemoryApplicationAttributeUpdater;
import org.cloudfoundry.multiapps.controller.process.util.ServiceOperationUtil;
import org.cloudfoundry.multiapps.controller.process.util.StagingApplicationAttributeUpdater;
import org.cloudfoundry.multiapps.controller.process.util.UrisApplicationAttributeUpdater;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.handlers.ArchiveHandler;
import org.cloudfoundry.multiapps.mta.util.NameUtil;
import org.flowable.engine.ProcessEngine;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.fasterxml.jackson.databind.JsonNode;
import com.sap.cloudfoundry.client.facade.ApplicationServicesUpdateCallback;
import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;

@Named("createOrUpdateAppStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CreateOrUpdateAppStep extends SyncFlowableStep {

    protected BooleanSupplier shouldPrettyPrint = () -> true;

    private final ProcessEngine processEngine;

    @Inject
    public CreateOrUpdateAppStep(ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }

    @Override
    protected StepPhase executeStep(ProcessContext context) throws FileStorageException {
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);

        CloudControllerClient client = context.getControllerClient();
        CloudApplication existingApp = client.getApplication(app.getName(), false);
        context.setVariable(Variables.EXISTING_APP, existingApp);

        StepFlowHandler flowHandler = createStepFlowHandler(context, client, app, existingApp);

        flowHandler.printStepStartMessage();

        flowHandler.handleApplicationAttributes();
        flowHandler.injectServiceKeysCredentialsInAppEnv();
        flowHandler.handleApplicationServices();
        flowHandler.handleApplicationEnv();
        flowHandler.handleApplicationMetadata();
        flowHandler.printStepEndMessage();

        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return MessageFormat.format(Messages.ERROR_CREATING_OR_UPDATING_APP, context.getVariable(Variables.APP_TO_PROCESS)
                                                                                    .getName());
    }

    private StepFlowHandler createStepFlowHandler(ProcessContext context, CloudControllerClient client, CloudApplicationExtended app,
                                                  CloudApplication existingApp) {
        if (shouldUseNewHandlers(context)) {
            if (existingApp == null) {
                return new CreateAppFlowHandler(context, client, app);
            }
            return new UpdateAppFlowHandler(context, client, app, existingApp);
        }

        if (existingApp == null) {
            return new OldCreateAppFlowHandler(context, client, app);
        }
        return new OldUpdateAppFlowHandler(context, client, app, existingApp);

    }

    private boolean shouldUseNewHandlers(ProcessContext context) {
        return getBindUnbindServicesCallActivity(context) != null;
    }

    protected JsonNode getBindUnbindServicesCallActivity(ProcessContext context) {
        return processEngine.getDynamicBpmnService()
                            .getDynamicProcessDefinitionSummary(context.getExecution()
                                                                       .getProcessDefinitionId())
                            .getSummary()
                            .get("manageAppServiceBindingCallActivity");
    }

    private abstract class StepFlowHandler {

        final ProcessContext context;
        CloudApplicationExtended app;
        final CloudControllerClient client;

        public StepFlowHandler(ProcessContext context, CloudControllerClient client, CloudApplicationExtended app) {
            this.context = context;
            this.app = app;
            this.client = client;
        }

        public void injectServiceKeysCredentialsInAppEnv() {
            Map<String, String> appEnv = new LinkedHashMap<>(app.getEnv());
            Map<String, String> appServiceKeysCredentials = buildServiceKeysCredentials(client, app, appEnv);
            app = ImmutableCloudApplicationExtended.copyOf(app)
                                                   .withEnv(appEnv);
            updateContextWithServiceKeysCredentials(app, appServiceKeysCredentials);
        }

        private Map<String, String> buildServiceKeysCredentials(CloudControllerClient client, CloudApplicationExtended app,
                                                                Map<String, String> appEnv) {
            Map<String, String> appServiceKeysCredentials = new HashMap<>();
            for (ServiceKeyToInject serviceKeyToInject : app.getServiceKeysToInject()) {
                String serviceKeyCredentials = JsonUtil.toJson(ServiceOperationUtil.getServiceKeyCredentials(client,
                                                                                                             serviceKeyToInject.getServiceName(),
                                                                                                             serviceKeyToInject.getServiceKeyName()),
                                                               shouldPrettyPrint.getAsBoolean());
                appEnv.put(serviceKeyToInject.getEnvVarName(), serviceKeyCredentials);
                appServiceKeysCredentials.put(serviceKeyToInject.getEnvVarName(), serviceKeyCredentials);
            }
            return appServiceKeysCredentials;
        }

        private void updateContextWithServiceKeysCredentials(CloudApplicationExtended app, Map<String, String> appServiceKeysCredentials) {
            Map<String, Map<String, String>> serviceKeysCredentialsToInject = context.getVariable(Variables.SERVICE_KEYS_CREDENTIALS_TO_INJECT);
            serviceKeysCredentialsToInject.put(app.getName(), appServiceKeysCredentials);

            // Update current process context
            context.setVariable(Variables.APP_TO_PROCESS, app);
            context.setVariable(Variables.SERVICE_KEYS_CREDENTIALS_TO_INJECT, serviceKeysCredentialsToInject);
        }

        protected Map<String, Map<String, Object>> getBindingParameters(ProcessContext context, CloudApplicationExtended app)
            throws FileStorageException {
            List<CloudServiceInstanceExtended> services = getServices(context.getVariable(Variables.SERVICES_TO_BIND), app.getServices());

            Map<String, Map<String, Object>> fileProvidedBindingParameters = getFileProvidedBindingParameters(context, app.getModuleName(),
                                                                                                              services);
            Map<String, Map<String, Object>> descriptorProvidedBindingParameters = ObjectUtils.defaultIfNull(app.getBindingParameters(),
                                                                                                             Collections.emptyMap());
            Map<String, Map<String, Object>> bindingParameters = mergeBindingParameters(descriptorProvidedBindingParameters,
                                                                                        fileProvidedBindingParameters);
            getStepLogger().debug(Messages.BINDING_PARAMETERS_FOR_APPLICATION, app.getName(),
                                  SecureSerialization.toJson(bindingParameters));
            return bindingParameters;
        }

        private List<CloudServiceInstanceExtended> getServices(List<CloudServiceInstanceExtended> services, List<String> serviceNames) {
            return services.stream()
                           .filter(service -> serviceNames.contains(service.getName()))
                           .collect(Collectors.toList());
        }

        private Map<String, Map<String, Object>> getFileProvidedBindingParameters(ProcessContext context, String moduleName,
                                                                                  List<CloudServiceInstanceExtended> services)
            throws FileStorageException {
            Map<String, Map<String, Object>> result = new TreeMap<>();
            for (CloudServiceInstanceExtended service : services) {
                String requiredDependencyName = NameUtil.getPrefixedName(moduleName, service.getResourceName(),
                                                                         org.cloudfoundry.multiapps.controller.core.Constants.MTA_ELEMENT_SEPARATOR);
                Map<String, Object> bindingParameters = getFileProvidedBindingParameters(context, requiredDependencyName);
                result.put(service.getName(), bindingParameters);
            }
            return result;
        }

        private Map<String, Object> getFileProvidedBindingParameters(ProcessContext context, String requiredDependencyName)
            throws FileStorageException {
            String archiveId = context.getRequiredVariable(Variables.APP_ARCHIVE_ID);
            MtaArchiveElements mtaArchiveElements = context.getVariable(Variables.MTA_ARCHIVE_ELEMENTS);
            String fileName = mtaArchiveElements.getRequiredDependencyFileName(requiredDependencyName);
            if (fileName == null) {
                return Collections.emptyMap();
            }
            FileContentProcessor<Map<String, Object>> fileProcessor = archive -> {
                try (InputStream file = ArchiveHandler.getInputStream(archive, fileName, configuration.getMaxManifestSize())) {
                    return JsonUtil.convertJsonToMap(file);
                } catch (IOException e) {
                    throw new SLException(e, Messages.ERROR_RETRIEVING_MTA_REQUIRED_DEPENDENCY_CONTENT, fileName);
                }
            };
            return fileService.processFileContent(context.getVariable(Variables.SPACE_GUID), archiveId, fileProcessor);
        }

        private Map<String, Map<String, Object>>
                mergeBindingParameters(Map<String, Map<String, Object>> descriptorProvidedBindingParameters,
                                       Map<String, Map<String, Object>> fileProvidedBindingParameters) {
            Map<String, Map<String, Object>> bindingParameters = new HashMap<>();
            Set<String> serviceNames = new HashSet<>(descriptorProvidedBindingParameters.keySet());
            serviceNames.addAll(fileProvidedBindingParameters.keySet());
            for (String serviceName : serviceNames) {
                bindingParameters.put(serviceName, MapUtil.mergeSafely(fileProvidedBindingParameters.get(serviceName),
                                                                       descriptorProvidedBindingParameters.get(serviceName)));
            }
            return bindingParameters;
        }

        protected Map<String, Object> getBindingParametersForService(String serviceName,
                                                                     Map<String, Map<String, Object>> bindingParameters) {
            return bindingParameters == null ? Collections.emptyMap() : bindingParameters.getOrDefault(serviceName, Collections.emptyMap());
        }

        public abstract void printStepStartMessage();

        public abstract void handleApplicationAttributes();

        public abstract void handleApplicationServices() throws FileStorageException;

        public abstract void handleApplicationEnv();

        public abstract void handleApplicationMetadata();

        public abstract void printStepEndMessage();
    }

    private class CreateAppFlowHandler extends StepFlowHandler {

        public CreateAppFlowHandler(ProcessContext context, CloudControllerClient client, CloudApplicationExtended app) {
            super(context, client, app);
        }

        @Override
        public void handleApplicationAttributes() {
            Integer diskQuota = (app.getDiskQuota() != 0) ? app.getDiskQuota() : null;
            Integer memory = (app.getMemory() != 0) ? app.getMemory() : null;
            List<String> uris = app.getUris();

            if (app.getDockerInfo() != null) {
                context.getStepLogger()
                       .info(Messages.CREATING_APP_FROM_DOCKER_IMAGE, app.getName(), app.getDockerInfo()
                                                                                        .getImage());
            }
            client.createApplication(app.getName(), app.getStaging(), diskQuota, memory, uris, app.getDockerInfo());
            context.setVariable(Variables.VCAP_APP_PROPERTIES_CHANGED, true);
        }

        @Override
        public void handleApplicationServices() throws FileStorageException {
            context.setVariable(Variables.SERVICES_TO_UNBIND_BIND, app.getServices());
        }

        @Override
        public void handleApplicationEnv() {
            client.updateApplicationEnv(app.getName(), app.getEnv());
            context.setVariable(Variables.USER_PROPERTIES_CHANGED, true);
        }

        @Override
        public void printStepStartMessage() {
            getStepLogger().info(Messages.CREATING_APP_FROM_MODULE, app.getName(), app.getModuleName());
        }

        @Override
        public void printStepEndMessage() {
            getStepLogger().debug(Messages.APP_CREATED, app.getName());
        }

        @Override
        public void handleApplicationMetadata() {
            /**
             * Do nothing. Update of application metadata was moved in new step {@link UpdateApplicationMetadataStep}. This was kept only
             * for backwards compatibility.
             **/
        }

    }

    private class OldCreateAppFlowHandler extends CreateAppFlowHandler {

        public OldCreateAppFlowHandler(ProcessContext context, CloudControllerClient client, CloudApplicationExtended app) {
            super(context, client, app);
        }

        @Override
        public void handleApplicationMetadata() {
            CloudApplication appFromController = client.getApplication(app.getName());
            client.updateApplicationMetadata(appFromController.getMetadata()
                                                              .getGuid(),
                                             app.getV3Metadata());
        }

        @Override
        public void handleApplicationServices() throws FileStorageException {
            List<String> services = app.getServices();
            Map<String, Map<String, Object>> bindingParameters = getBindingParameters(context, app);
            for (String serviceName : services) {
                Map<String, Object> bindingParametersForCurrentService = getBindingParametersForService(serviceName, bindingParameters);
                bindServiceInstance(context, client, app.getName(), serviceName, bindingParametersForCurrentService);
            }
            context.setVariable(Variables.VCAP_SERVICES_PROPERTIES_CHANGED, true);
        }

        private void bindServiceInstance(ProcessContext context, CloudControllerClient client, String appName, String serviceName,
                                         Map<String, Object> bindingParameters) {
            client.bindServiceInstance(appName, serviceName, bindingParameters, getApplicationServicesUpdateCallback(context));
        }

    }

    private class UpdateAppFlowHandler extends StepFlowHandler {

        final CloudApplication existingApp;

        public UpdateAppFlowHandler(ProcessContext context, CloudControllerClient client, CloudApplicationExtended app,
                                    CloudApplication existingApp) {
            super(context, client, app);
            this.existingApp = existingApp;
        }

        @Override
        public void handleApplicationAttributes() {
            List<UpdateState> updateStates = getApplicationAttributeUpdaters().stream()
                                                                              .map(updater -> updater.update(existingApp, app))
                                                                              .collect(Collectors.toList());

            boolean arePropertiesChanged = updateStates.contains(UpdateState.UPDATED);

            reportApplicationUpdateStatus(app, arePropertiesChanged);
            context.setVariable(Variables.VCAP_APP_PROPERTIES_CHANGED, arePropertiesChanged);
        }

        @Override
        public void handleApplicationServices() throws FileStorageException {
            if (context.getVariable(Variables.SHOULD_SKIP_SERVICE_REBINDING)) {
                return;
            }
            List<String> services = getMtaAndExistingSevices();
            context.setVariable(Variables.SERVICES_TO_UNBIND_BIND, services);
        }

        @Override
        public void handleApplicationEnv() {
            Map<String, String> envAsMap = new LinkedHashMap<>(app.getEnv());
            updateAppDigest(envAsMap, existingApp.getEnv());
            app = ImmutableCloudApplicationExtended.copyOf(app)
                                                   .withEnv(envAsMap);

            UpdateState updateApplicationEnvironmentState = updateApplicationEnvironment(app, existingApp, client,
                                                                                         app.getAttributesUpdateStrategy());

            context.setVariable(Variables.USER_PROPERTIES_CHANGED, updateApplicationEnvironmentState == UpdateState.UPDATED);
        }

        @Override
        public void printStepStartMessage() {
            getStepLogger().info(Messages.UPDATING_APP, app.getName());
        }

        @Override
        public void printStepEndMessage() {
            getStepLogger().debug(Messages.APP_UPDATED, app.getName());
        }

        private List<String> getMtaAndExistingSevices() {
            return Stream.of(app.getServices(), existingApp.getServices())
                         .flatMap(List::stream)
                         .distinct()
                         .collect(Collectors.toList());
        }

        private UpdateState
                updateApplicationEnvironment(CloudApplicationExtended app, CloudApplication existingApp, CloudControllerClient client,
                                             CloudApplicationExtended.AttributeUpdateStrategy applicationAttributesUpdateStrategy) {
            ControllerClientFacade.Context context = new ControllerClientFacade.Context(client, getStepLogger());
            return new EnvironmentApplicationAttributeUpdater(context,
                                                              getUpdateStrategy(applicationAttributesUpdateStrategy.shouldKeepExistingEnv())).update(existingApp,
                                                                                                                                                     app);
        }

        @Override
        public void handleApplicationMetadata() {
            /**
             * Do nothing. Update of application metadata was moved in new step {@link UpdateApplicationMetadataStep}. This was kept only
             * for backwards compatibility.
             **/
        }

        private void reportApplicationUpdateStatus(CloudApplicationExtended app, boolean appPropertiesChanged) {
            if (!appPropertiesChanged) {
                getStepLogger().info(Messages.APPLICATION_ATTRIBUTES_UNCHANGED, app.getName());
                return;
            }
            getStepLogger().debug(Messages.APP_UPDATED, app.getName());
        }

        private List<ApplicationAttributeUpdater> getApplicationAttributeUpdaters() {
            ControllerClientFacade.Context context = new ControllerClientFacade.Context(client, getStepLogger());
            return Arrays.asList(new StagingApplicationAttributeUpdater(context), new MemoryApplicationAttributeUpdater(context),
                                 new DiskQuotaApplicationAttributeUpdater(context),
                                 new UrisApplicationAttributeUpdater(context, UpdateStrategy.REPLACE));
        }

        private UpdateStrategy getUpdateStrategy(boolean shouldKeepAttributes) {
            return shouldKeepAttributes ? UpdateStrategy.MERGE : UpdateStrategy.REPLACE;
        }

        private void updateAppDigest(Map<String, String> newAppEnv, Map<String, String> existingAppEnv) {
            Object existingFileDigest = getExistingAppFileDigest(existingAppEnv);
            if (existingFileDigest == null) {
                return;
            }
            String newAppDeployAttributes = newAppEnv.get(org.cloudfoundry.multiapps.controller.core.Constants.ENV_DEPLOY_ATTRIBUTES);
            TreeMap<String, Object> newAppDeployAttributesMap = new TreeMap<>(JsonUtil.convertJsonToMap(newAppDeployAttributes));
            newAppDeployAttributesMap.put(org.cloudfoundry.multiapps.controller.core.Constants.ATTR_APP_CONTENT_DIGEST, existingFileDigest);
            newAppEnv.put(org.cloudfoundry.multiapps.controller.core.Constants.ENV_DEPLOY_ATTRIBUTES,
                          JsonUtil.toJson(newAppDeployAttributesMap, shouldPrettyPrint.getAsBoolean()));
        }

        private Object getExistingAppFileDigest(Map<String, String> envAsMap) {
            return new ApplicationFileDigestDetector(envAsMap).detectCurrentAppFileDigest();
        }

    }

    private class OldUpdateAppFlowHandler extends UpdateAppFlowHandler {

        public OldUpdateAppFlowHandler(ProcessContext context, CloudControllerClient client, CloudApplicationExtended app,
                                       CloudApplication existingApp) {
            super(context, client, app, existingApp);
        }

        @Override
        public void handleApplicationMetadata() {
            if (app.getV3Metadata() == null) {
                return;
            }
            boolean shouldUpdateMetadata = true;
            if (existingApp.getV3Metadata() != null) {
                shouldUpdateMetadata = !existingApp.getV3Metadata()
                                                   .equals(app.getV3Metadata());
            }
            if (shouldUpdateMetadata) {
                client.updateApplicationMetadata(existingApp.getMetadata()
                                                            .getGuid(),
                                                 app.getV3Metadata());
            }
        }

        @Override
        public void handleApplicationServices() throws FileStorageException {
            if (context.getVariable(Variables.SHOULD_SKIP_SERVICE_REBINDING)) {
                return;
            }

            List<String> services = app.getServices();
            boolean hasUnboundServices = unbindServicesIfNeeded(app, existingApp, client, services);

            Map<String, Map<String, Object>> bindingParameters = getBindingParameters(context, app);

            boolean hasUpdatedServices = updateServices(context, app.getName(), bindingParameters, client,
                                                        calculateServicesForUpdate(app, existingApp.getServices()));

            context.setVariable(Variables.VCAP_SERVICES_PROPERTIES_CHANGED, hasUnboundServices || hasUpdatedServices);
        }

        private boolean unbindServicesIfNeeded(CloudApplicationExtended application, CloudApplication existingApplication,
                                               CloudControllerClient client, List<String> services) {
            AttributeUpdateStrategy applicationAttributesUpdateBehavior = application.getAttributesUpdateStrategy();
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
            for (String serviceName : servicesToUnbind) {
                client.unbindServiceInstance(app.getName(), serviceName);
            }
            return !servicesToUnbind.isEmpty();
        }

        private Set<String> calculateServicesForUpdate(CloudApplicationExtended application, List<String> existingServices) {
            AttributeUpdateStrategy applicationAttributesUpdateBehavior = application.getAttributesUpdateStrategy();
            Set<String> servicesForUpdate = new HashSet<>(application.getServices());
            if (!applicationAttributesUpdateBehavior.shouldKeepExistingServiceBindings()) {
                return servicesForUpdate;
            }
            servicesForUpdate.addAll(existingServices);
            return servicesForUpdate;
        }

        private boolean updateServices(ProcessContext context, String applicationName, Map<String, Map<String, Object>> bindingParameters,
                                       CloudControllerClient client, Set<String> services) {
            Map<String, Map<String, Object>> serviceNamesWithBindingParameters = services.stream()
                                                                                         .collect(Collectors.toMap(String::toString,
                                                                                                                   serviceName -> getBindingParametersForService(serviceName,
                                                                                                                                                                 bindingParameters)));
            ApplicationServicesUpdater applicationServicesUpdater = new ApplicationServicesUpdater(new ControllerClientFacade.Context(client,
                                                                                                                                      getStepLogger()));
            List<String> updatedServices = applicationServicesUpdater.updateApplicationServices(applicationName,
                                                                                                serviceNamesWithBindingParameters,
                                                                                                getApplicationServicesUpdateCallback(context));

            reportNonUpdatedServices(services, applicationName, updatedServices);
            return !updatedServices.isEmpty();
        }

        private void reportNonUpdatedServices(Set<String> services, String applicationName, List<String> updatedServices) {
            List<String> nonUpdatesServices = ListUtils.removeAll(services, updatedServices);
            for (String service : nonUpdatesServices) {
                getStepLogger().warn(Messages.WILL_NOT_REBIND_APP_TO_SERVICE, service, applicationName);
            }
        }

    }

    protected ApplicationServicesUpdateCallback getApplicationServicesUpdateCallback(ProcessContext context) {
        return new BindServiceToApplicationStep.DefaultApplicationServicesUpdateCallback(context);
    }

}
