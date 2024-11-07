package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ServiceKeyToInject;
import org.cloudfoundry.multiapps.controller.core.cf.clients.AppBoundServiceInstanceNamesGetter;
import org.cloudfoundry.multiapps.controller.core.cf.clients.WebClientFactory;
import org.cloudfoundry.multiapps.controller.core.helpers.ApplicationFileDigestDetector;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ApplicationAttributeUpdater;
import org.cloudfoundry.multiapps.controller.process.util.ApplicationAttributeUpdater.UpdateState;
import org.cloudfoundry.multiapps.controller.process.util.ControllerClientFacade;
import org.cloudfoundry.multiapps.controller.process.util.DiskQuotaApplicationAttributeUpdater;
import org.cloudfoundry.multiapps.controller.process.util.ElementUpdater.UpdateStrategy;
import org.cloudfoundry.multiapps.controller.process.util.EnvironmentApplicationAttributeUpdater;
import org.cloudfoundry.multiapps.controller.process.util.MemoryApplicationAttributeUpdater;
import org.cloudfoundry.multiapps.controller.process.util.StagingApplicationAttributeUpdater;
import org.cloudfoundry.multiapps.controller.process.util.UrisApplicationAttributeUpdater;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudCredentials;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.dto.ApplicationToCreateDto;
import com.sap.cloudfoundry.client.facade.dto.ImmutableApplicationToCreateDto;

import static org.cloudfoundry.multiapps.controller.process.steps.StepsUtil.disableAutoscaling;

@Named("createOrUpdateAppStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CreateOrUpdateAppStep extends SyncFlowableStep {

    protected BooleanSupplier shouldPrettyPrint = () -> true;

    @Inject
    private TokenService tokenService;
    @Inject
    private WebClientFactory webClientFactory;

    @Override
    protected StepPhase executeStep(ProcessContext context) throws FileStorageException {
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
        CloudControllerClient client = context.getControllerClient();
        CloudApplication existingApp = client.getApplication(app.getName(), false);
        context.setVariable(Variables.EXISTING_APP, existingApp);

        StepFlowHandler flowHandler = createStepFlowHandler(context, client, app, existingApp);

        flowHandler.printStepStartMessage();

        flowHandler.injectServiceKeysCredentialsInAppEnv();
        flowHandler.handleApplicationAttributes();
        flowHandler.handleApplicationServices();
        flowHandler.printStepEndMessage();

        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return MessageFormat.format(Messages.ERROR_CREATING_OR_UPDATING_APP, context.getVariable(Variables.APP_TO_PROCESS)
                                                                                    .getName());
    }

    protected AppBoundServiceInstanceNamesGetter getAppBoundServiceInstanceNamesGetter(ProcessContext context) {
        String user = context.getVariable(Variables.USER);
        String correlationId = context.getVariable(Variables.CORRELATION_ID);
        var token = tokenService.getToken(user);
        var credentials = new CloudCredentials(token, true);
        return new AppBoundServiceInstanceNamesGetter(configuration, webClientFactory, credentials, correlationId);
    }

    private StepFlowHandler createStepFlowHandler(ProcessContext context, CloudControllerClient client, CloudApplicationExtended app,
                                                  CloudApplication existingApp) {
        if (existingApp == null) {
            return new CreateAppFlowHandler(context, client, app);
        }
        return new UpdateAppFlowHandler(context, client, app, existingApp);
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
            Map<String, String> appServiceKeysCredentials = buildServiceKeysCredentials(client, app);
            appEnv.putAll(appServiceKeysCredentials);
            app = ImmutableCloudApplicationExtended.copyOf(app)
                                                   .withEnv(appEnv);
            context.setVariable(Variables.APP_TO_PROCESS, app);
        }

        private Map<String, String> buildServiceKeysCredentials(CloudControllerClient client, CloudApplicationExtended app) {
            Map<String, String> serviceKeys = new HashMap<>();
            for (ServiceKeyToInject serviceKeyToInject : app.getServiceKeysToInject()) {
                var serviceKey = client.getServiceKey(serviceKeyToInject.getServiceName(), serviceKeyToInject.getServiceKeyName());
                String serviceKeyCredentials = JsonUtil.toJson(serviceKey.getCredentials(), shouldPrettyPrint.getAsBoolean());
                serviceKeys.put(serviceKeyToInject.getEnvVarName(), serviceKeyCredentials);
            }
            return serviceKeys;
        }

        public abstract void printStepStartMessage();

        public abstract void handleApplicationAttributes();

        public abstract void handleApplicationServices();

        public abstract void printStepEndMessage();
    }

    private class CreateAppFlowHandler extends StepFlowHandler {

        public CreateAppFlowHandler(ProcessContext context, CloudControllerClient client, CloudApplicationExtended app) {
            super(context, client, app);
        }

        @Override
        public void handleApplicationAttributes() {
            Integer diskQuotaInMb = (app.getDiskQuota() != 0) ? app.getDiskQuota() : null;
            Integer memoryInMb = (app.getMemory() != 0) ? app.getMemory() : null;
            if (app.getDockerInfo() != null) {
                context.getStepLogger()
                       .info(Messages.CREATING_APP_FROM_DOCKER_IMAGE, app.getName(), app.getDockerInfo()
                                                                                        .getImage());
            }
            ApplicationToCreateDto applicationToCreateDto = ImmutableApplicationToCreateDto.builder()
                                                                                           .name(app.getName())
                                                                                           .staging(app.getStaging())
                                                                                           .diskQuotaInMb(diskQuotaInMb)
                                                                                           .memoryInMb(memoryInMb)
                                                                                           .metadata(app.getV3Metadata())
                                                                                           .routes(app.getRoutes())
                                                                                           .env(app.getEnv())
                                                                                           .build();
            client.createApplication(applicationToCreateDto);
            addMetadataAutoscalerLabel();
            context.setVariable(Variables.VCAP_APP_PROPERTIES_CHANGED, true);
            context.setVariable(Variables.USER_PROPERTIES_CHANGED, true);
        }

        @Override
        public void handleApplicationServices() {
            context.setVariable(Variables.SERVICES_TO_UNBIND_BIND, app.getServices());
        }

        @Override
        public void printStepStartMessage() {
            getStepLogger().info(Messages.CREATING_APP_FROM_MODULE, app.getName(), app.getModuleName());
        }

        @Override
        public void printStepEndMessage() {
            getStepLogger().debug(Messages.APP_CREATED, app.getName());
        }

        private void addMetadataAutoscalerLabel() {
            boolean shouldApplyIncrementalInstancesUpdate = context.getVariable(Variables.SHOULD_APPLY_INCREMENTAL_INSTANCES_UPDATE);
            if (shouldApplyIncrementalInstancesUpdate) {
                UUID applicationId = client.getApplicationGuid(app.getName());
                disableAutoscaling(context, client, applicationId);
            }
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
            updateMetadata();

            List<UpdateState> updateStates = getApplicationAttributeUpdaters().stream()
                                                                              .map(updater -> updater.update(existingApp, app))
                                                                              .collect(Collectors.toList());

            boolean arePropertiesChanged = updateStates.contains(UpdateState.UPDATED);

            reportApplicationUpdateStatus(app, arePropertiesChanged);
            context.setVariable(Variables.VCAP_APP_PROPERTIES_CHANGED, arePropertiesChanged);
            updateApplicationEnvironment();
        }

        private void updateApplicationEnvironment() {
            Map<String, String> envAsMap = new LinkedHashMap<>(app.getEnv());
            var appEnv = client.getApplicationEnvironment(existingApp.getGuid());
            addCurrentAppDigestToNewEnv(envAsMap, appEnv);
            app = ImmutableCloudApplicationExtended.copyOf(app)
                                                   .withEnv(envAsMap);

            ControllerClientFacade.Context clientContext = new ControllerClientFacade.Context(client, context, getStepLogger());
            UpdateState updateApplicationEnvironmentState = new EnvironmentApplicationAttributeUpdater(clientContext,
                                                                                                       getEnvUpdateStrategy(),
                                                                                                       appEnv).update(existingApp, app);

            context.setVariable(Variables.USER_PROPERTIES_CHANGED, updateApplicationEnvironmentState == UpdateState.UPDATED);
        }

        private void updateMetadata() {
            if (app.getV3Metadata() == null) {
                return;
            }
            if (!Objects.equals(existingApp.getV3Metadata(), app.getV3Metadata())) {
                client.updateApplicationMetadata(existingApp.getGuid(), app.getV3Metadata());
            }
        }

        private void addCurrentAppDigestToNewEnv(Map<String, String> newAppEnv, Map<String, String> existingAppEnv) {
            String existingFileDigest = getExistingAppFileDigest(existingAppEnv);
            if (existingFileDigest == null) {
                return;
            }
            String newAppDeployAttributes = newAppEnv.get(org.cloudfoundry.multiapps.controller.core.Constants.ENV_DEPLOY_ATTRIBUTES);
            TreeMap<String, Object> newAppDeployAttributesMap = new TreeMap<>(JsonUtil.convertJsonToMap(newAppDeployAttributes));
            newAppDeployAttributesMap.put(org.cloudfoundry.multiapps.controller.core.Constants.ATTR_APP_CONTENT_DIGEST, existingFileDigest);
            newAppEnv.put(org.cloudfoundry.multiapps.controller.core.Constants.ENV_DEPLOY_ATTRIBUTES,
                          JsonUtil.toJson(newAppDeployAttributesMap, shouldPrettyPrint.getAsBoolean()));
        }

        private String getExistingAppFileDigest(Map<String, String> envAsMap) {
            return new ApplicationFileDigestDetector(envAsMap).detectCurrentAppFileDigest();
        }

        private UpdateStrategy getEnvUpdateStrategy() {
            return app.getAttributesUpdateStrategy()
                      .shouldKeepExistingEnv() ? UpdateStrategy.MERGE : UpdateStrategy.REPLACE;
        }

        @Override
        public void handleApplicationServices() {
            if (context.getVariable(Variables.SHOULD_SKIP_SERVICE_REBINDING)) {
                return;
            }
            List<String> services = getMtaAndExistingServices();
            context.setVariable(Variables.SERVICES_TO_UNBIND_BIND, services);
        }

        @Override
        public void printStepStartMessage() {
            getStepLogger().info(Messages.UPDATING_APP, app.getName());
        }

        @Override
        public void printStepEndMessage() {
            getStepLogger().debug(Messages.APP_UPDATED, app.getName());
        }

        private List<String> getMtaAndExistingServices() {
            var serviceNamesGetter = getAppBoundServiceInstanceNamesGetter(context);
            return Stream.of(app.getServices(), serviceNamesGetter.getServiceInstanceNamesBoundToApp(existingApp.getGuid()))
                         .flatMap(List::stream)
                         .distinct()
                         .collect(Collectors.toList());
        }

        private void reportApplicationUpdateStatus(CloudApplicationExtended app, boolean appPropertiesChanged) {
            if (!appPropertiesChanged) {
                getStepLogger().info(Messages.APPLICATION_ATTRIBUTES_UNCHANGED, app.getName());
                return;
            }
            getStepLogger().debug(Messages.APP_UPDATED, app.getName());
        }

        protected List<ApplicationAttributeUpdater> getApplicationAttributeUpdaters() {
            ControllerClientFacade.Context clientContext = new ControllerClientFacade.Context(client, context, getStepLogger());
            var process = client.getApplicationProcess(existingApp.getGuid());
            var currentRoutes = client.getApplicationRoutes(existingApp.getGuid());
            context.setVariable(Variables.CURRENT_ROUTES, currentRoutes);
            return List.of(new StagingApplicationAttributeUpdater(clientContext, process),
                           new MemoryApplicationAttributeUpdater(clientContext, process),
                           new DiskQuotaApplicationAttributeUpdater(clientContext, process),
                           new UrisApplicationAttributeUpdater(clientContext, UpdateStrategy.REPLACE, currentRoutes));
        }

    }

}
