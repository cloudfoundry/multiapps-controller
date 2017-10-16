package com.sap.cloud.lm.sl.cf.process.steps;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.ServiceKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.DefaultTagsDetector;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceCreator;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceUpdater;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ServiceOperationExecutor;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.mta.handlers.ArchiveHandler;
import com.sap.cloud.lm.sl.mta.util.PropertiesUtil;
import com.sap.cloud.lm.sl.persistence.processors.DefaultFileDownloadProcessor;
import com.sap.cloud.lm.sl.persistence.services.FileContentProcessor;
import com.sap.cloud.lm.sl.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("createOrUpdateServicesStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CreateOrUpdateServicesStep extends AbstractXS2ProcessStepWithBridge {

    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    private ServiceOperationExecutor serviceOperationExecutor = new ServiceOperationExecutor();

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("createOrUpdateServicesTask").displayName("Create Or Update Services").description(
            "Create Or Update Services").children(Arrays.asList(PollServiceOperationsStep.getMetadata())).build();
    }

    @Autowired
    protected DefaultTagsDetector defaultTagsDetector;

    @Autowired
    protected ServiceCreator serviceCreator;

    @Autowired
    @Qualifier("serviceUpdater")
    protected ServiceUpdater serviceUpdater;

    @Override
    protected ExecutionStatus pollStatusInternal(DelegateExecution context) throws SLException, FileStorageException {

        getStepLogger().logActivitiTask();
        try {
            getStepLogger().info(Messages.CREATING_OR_UPDATING_SERVICES);

            CloudFoundryOperations client = getCloudFoundryClient(context);
            Map<String, List<String>> defaultTags = defaultTagsDetector.computeDefaultTags(client);
            getStepLogger().debug("Default tags: " + JsonUtil.toJson(defaultTags, true));

            List<CloudService> existingServices = client.getServices();
            Map<String, CloudService> existingServicesMap = getServicesMap(existingServices);
            getStepLogger().debug("Existing services: " + existingServicesMap.keySet());

            List<CloudServiceExtended> services = StepsUtil.getServicesToCreate(context);
            Map<String, List<ServiceKey>> serviceKeys = StepsUtil.getServiceKeysToCreate(context);

            Map<String, ServiceOperationType> triggeredServiceOperations = createOrUpdateServices(context, client, services,
                existingServicesMap, serviceKeys, defaultTags);
            getStepLogger().debug(Messages.TRIGGERED_SERVICE_OPERATIONS, JsonUtil.toJson(triggeredServiceOperations, true));
            StepsUtil.setTriggeredServiceOperations(context, triggeredServiceOperations);

            getStepLogger().debug(Messages.SERVICES_CREATED_OR_UPDATED);
            return ExecutionStatus.SUCCESS;
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            getStepLogger().error(e, Messages.ERROR_CREATING_SERVICES);
            throw e;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_CREATING_SERVICES);
            throw e;
        }
    }

    private Map<String, CloudService> getServicesMap(List<CloudService> services) {
        Map<String, CloudService> servicesMap = new HashMap<>(services.size());
        services.forEach(service -> servicesMap.put(service.getName(), service));
        return servicesMap;
    }

    private Map<String, ServiceOperationType> createOrUpdateServices(DelegateExecution context, CloudFoundryOperations client,
        List<CloudServiceExtended> services, Map<String, CloudService> existingServices, Map<String, List<ServiceKey>> serviceKeys,
        Map<String, List<String>> defaultTags) throws SLException, FileStorageException {

        Map<String, ServiceOperationType> triggeredOperations = new TreeMap<>();
        for (CloudServiceExtended service : services) {
            CloudService existingService = existingServices.get(service.getName());
            List<String> defaultTagsForService = defaultTags.get(service.getLabel());
            ServiceOperationType triggeredOperation = createOrUpdateService(context, client, service, existingService,
                defaultTagsForService);
            triggeredOperations.put(service.getName(), triggeredOperation);
            List<ServiceKey> serviceKeysForService = serviceKeys.getOrDefault(service.getName(), Collections.emptyList());
            createOrUpdateServiceKeys(serviceKeysForService, service, existingService, client, context);
        }
        return triggeredOperations;
    }

    private void createOrUpdateServiceKeys(List<ServiceKey> serviceKeys, CloudServiceExtended service, CloudService existingService,
        CloudFoundryOperations client, DelegateExecution context) throws SLException {
        // TODO: Do not use client extensions when the CF Java Client we use supports managing of
        // service keys.
        ClientExtensions clientExtensions = getClientExtensions(context);
        if (clientExtensions == null) {
            return;
        }
        // User provided services cannot have service keys.
        if (service.isUserProvided()) {
            return;
        }
        List<ServiceKey> existingServiceKeys = serviceOperationExecutor.executeServiceOperation(service,
            () -> client.getServiceKeys(service.getName()), getStepLogger());

        if (existingServiceKeys == null) {
            return;
        }

        List<ServiceKey> serviceKeysToCreate = getServiceKeysToCreate(serviceKeys, existingServiceKeys);
        List<ServiceKey> serviceKeysToUpdate = getServiceKeysToUpdate(serviceKeys, existingServiceKeys);
        List<ServiceKey> serviceKeysToDelete = getServiceKeysToDelete(serviceKeys, existingServiceKeys);

        if (canDeleteServiceKeys(context)) {
            deleteServiceKeys(clientExtensions, serviceKeysToDelete);
            // Recreate the service keys, which should be updated, as direct update is not supported
            // by the controller:
            deleteServiceKeys(clientExtensions, serviceKeysToUpdate);
            createServiceKeys(clientExtensions, serviceKeysToUpdate);
        } else {
            serviceKeysToDelete.forEach((key) -> {
                getStepLogger().warn(Messages.WILL_NOT_DELETE_SERVICE_KEY, key.getName(), key.getService().getName());
            });
            serviceKeysToUpdate.forEach((key) -> {
                getStepLogger().warn(Messages.WILL_NOT_UPDATE_SERVICE_KEY, key.getName(), key.getService().getName());
            });
        }
        createServiceKeys(clientExtensions, serviceKeysToCreate);
    }

    private boolean canDeleteServiceKeys(DelegateExecution context) {
        return (Boolean) context.getVariable(Constants.PARAM_DELETE_SERVICE_KEYS);
    }

    private List<ServiceKey> getServiceKeysToCreate(List<ServiceKey> serviceKeys, List<ServiceKey> existingServiceKeys) {
        return serviceKeys.stream().filter(key -> shouldCreate(key, existingServiceKeys)).collect(Collectors.toList());
    }

    private List<ServiceKey> getServiceKeysToUpdate(List<ServiceKey> serviceKeys, List<ServiceKey> existingServiceKeys) {
        return serviceKeys.stream().filter(key -> shouldUpdate(key, existingServiceKeys)).collect(Collectors.toList());
    }

    private List<ServiceKey> getServiceKeysToDelete(List<ServiceKey> serviceKeys, List<ServiceKey> existingServiceKeys) {
        return existingServiceKeys.stream().filter(key -> shouldDelete(key, serviceKeys)).collect(Collectors.toList());
    }

    private boolean shouldCreate(ServiceKey key, List<ServiceKey> existingKeys) {
        return getWithName(existingKeys, key.getName()) == null;
    }

    private boolean shouldUpdate(ServiceKey key, List<ServiceKey> existingKeys) {
        ServiceKey existingKey = getWithName(existingKeys, key.getName());
        return (existingKey != null) && (!areServiceKeysEqual(key, existingKey));
    }

    private boolean shouldDelete(ServiceKey existingKey, List<ServiceKey> keys) {
        return getWithName(keys, existingKey.getName()) == null;
    }

    private ServiceKey getWithName(List<ServiceKey> serviceKeys, String name) {
        return serviceKeys.stream().filter(key -> key.getName().equals(name)).findAny().orElse(null);
    }

    private boolean areServiceKeysEqual(ServiceKey key1, ServiceKey key2) {
        return Objects.equals(key1.getParameters(), key2.getParameters()) && Objects.equals(key1.getName(), key2.getName());
    }

    private void deleteServiceKeys(ClientExtensions client, List<ServiceKey> serviceKeys) {
        serviceKeys.stream().forEach(key -> deleteServiceKey(client, key));
    }

    private void createServiceKeys(ClientExtensions client, List<ServiceKey> serviceKeys) {
        serviceKeys.stream().forEach(key -> createServiceKey(client, key));
    }

    private void createServiceKey(ClientExtensions client, ServiceKey key) {
        getStepLogger().info(Messages.CREATING_SERVICE_KEY_FOR_SERVICE, key.getName(), key.getService().getName());
        client.createServiceKey(key.getService().getName(), key.getName(), JsonUtil.toJson(key.getParameters()));
    }

    private void deleteServiceKey(ClientExtensions client, ServiceKey key) {
        getStepLogger().info(Messages.DELETING_SERVICE_KEY_FOR_SERVICE, key.getName(), key.getService().getName());
        client.deleteServiceKey(key.getService().getName(), key.getName());
    }

    private ServiceOperationType createOrUpdateService(DelegateExecution context, CloudFoundryOperations client,
        CloudServiceExtended service, CloudService existingService, List<String> defaultTags) throws SLException, FileStorageException {

        // Set service parameters if a file containing their values exists:
        String fileName = StepsUtil.getResourceFileName(context, service.getResourceName());
        if (fileName != null) {
            getStepLogger().debug(Messages.SETTING_SERVICE_PARAMETERS, service.getName(), fileName);
            String appArchiveId = StepsUtil.getRequiredStringParameter(context, Constants.PARAM_APP_ARCHIVE_ID);
            setServiceParameters(context, service, appArchiveId, fileName);
        }

        if (existingService == null) {
            serviceOperationExecutor.executeServiceOperation(service, () -> createService(context, client, service), getStepLogger());
            return ServiceOperationType.CREATE;
        }

        getStepLogger().debug(Messages.SERVICE_ALREADY_EXISTS, service.getName());
        List<ServiceAction> actions = determineActions(client, service, existingService, defaultTags);
        if (actions.contains(ServiceAction.ACTION_RECREATE)) {
            boolean deleteAllowed = (boolean) context.getVariable(Constants.PARAM_DELETE_SERVICES);
            if (!deleteAllowed) {
                getStepLogger().warn(Messages.WILL_NOT_RECREATE_SERVICE, service.getName());
                return null;
            }
            serviceOperationExecutor.executeServiceOperation(service, () -> deleteService(context, client, service), getStepLogger());
            serviceOperationExecutor.executeServiceOperation(service, () -> createService(context, client, service), getStepLogger());
            return ServiceOperationType.UPDATE;
        }
        ServiceOperationType type = null;
        if (actions.contains(ServiceAction.ACTION_UPDATE_SERVICE_PLAN)) {
            serviceOperationExecutor.executeServiceOperation(service, () -> updateServicePlan(context, client, service), getStepLogger());
            type = ServiceOperationType.UPDATE;
        }

        if (actions.contains(ServiceAction.ACTION_UPDATE_CREDENTIALS)) {
            serviceOperationExecutor.executeServiceOperation(service, () -> updateServiceCredentials(context, client, service),
                getStepLogger());
            type = ServiceOperationType.UPDATE;
        }
        if (actions.contains(ServiceAction.ACTION_UPDATE_TAGS)) {
            serviceOperationExecutor.executeServiceOperation(service, () -> updateServiceTags(context, client, service), getStepLogger());
            type = ServiceOperationType.UPDATE;
        }

        if (actions.isEmpty()) {
            getStepLogger().info(Messages.SERVICE_UNCHANGED, existingService.getName());
        }

        return type;
    }

    private void updateServicePlan(DelegateExecution context, CloudFoundryOperations client, CloudServiceExtended service) {
        getStepLogger().debug(
            MessageFormat.format("Updating service plan of a service {0} with new plan: {1}", service.getName(), service.getPlan()));
        serviceUpdater.updateServicePlan(client, service.getName(), service.getPlan());
    }

    private List<ServiceAction> determineActions(CloudFoundryOperations client, CloudServiceExtended service, CloudService existingService,
        List<String> defaultTags) {
        List<ServiceAction> actions = new ArrayList<>();

        getStepLogger().debug("Determining action to be performed on existing service...");

        // Check if the existing service should be updated or not
        if (shouldRecreate(service, existingService)) {
            getStepLogger().debug("Service should be recreated");
            getStepLogger().debug("New service: " + secureSerializer.toJson(service));
            getStepLogger().debug("Existing service: " + secureSerializer.toJson(existingService));
            return Arrays.asList(ServiceAction.ACTION_RECREATE);
        }

        if (shouldUpdatePlan(service, existingService)) {
            getStepLogger().debug("Service plan should be updated");
            getStepLogger().debug(MessageFormat.format("New service plan: {0}", service.getPlan()));
            getStepLogger().debug(MessageFormat.format("Existing service plan: {0}", existingService.getPlan()));
            actions.add(ServiceAction.ACTION_UPDATE_SERVICE_PLAN);
        }

        CloudServiceInstance existingServiceInstance = client.getServiceInstance(service.getName(), false);
        if (existingServiceInstance != null && shouldUpdateCredentials(service, existingServiceInstance.getCredentials())) {
            getStepLogger().debug("Service parameters should be updated");
            getStepLogger().debug("New parameters: " + secureSerializer.toJson(service.getCredentials()));
            getStepLogger().debug("Existing service parameters: " + secureSerializer.toJson(existingServiceInstance.getCredentials()));
            actions.add(ServiceAction.ACTION_UPDATE_CREDENTIALS);
        }

        if (shouldUpdateTags(service, existingService, defaultTags)) {
            CloudServiceExtended existingServiceExtended = (CloudServiceExtended) existingService;
            getStepLogger().debug("Service tags should be updated");
            getStepLogger().debug("New service tags: " + JsonUtil.toJson(service.getTags()));
            getStepLogger().debug("Existing service tags: " + JsonUtil.toJson(existingServiceExtended.getTags()));
            actions.add(ServiceAction.ACTION_UPDATE_TAGS);
        }

        return actions;
    }

    private boolean shouldUpdatePlan(CloudServiceExtended service, CloudService existingService) {
        return !Objects.equals(service.getPlan(), existingService.getPlan());
    }

    private void createService(DelegateExecution context, CloudFoundryOperations client, CloudServiceExtended service) {
        getStepLogger().info(Messages.CREATING_SERVICE, service.getName());
        if (service.isUserProvided()) {
            client.createUserProvidedService(service, service.getCredentials());
        } else {
            serviceCreator.createService(client, service, StepsUtil.getSpaceId(context));
        }
        getStepLogger().debug(Messages.SERVICE_CREATED, service.getName());
    }

    private void updateServiceTags(DelegateExecution context, CloudFoundryOperations client, CloudServiceExtended service)
        throws SLException {
        ClientExtensions clientExtensions = getClientExtensions(context);
        // TODO: Remove the service.isUserProvided() check when user provided services support tags.
        // See the following issue for more info:
        // https://www.pivotaltracker.com/n/projects/966314/stories/105674948
        if (clientExtensions == null || service.isUserProvided())
            return;
        getStepLogger().info(Messages.UPDATING_SERVICE_TAGS, service.getName());
        clientExtensions.updateServiceTags(service.getName(), service.getTags());
        getStepLogger().debug(Messages.SERVICE_TAGS_UPDATED, service.getName());
    }

    private void updateServiceCredentials(DelegateExecution context, CloudFoundryOperations client, CloudServiceExtended service)
        throws SLException {
        getStepLogger().info(Messages.UPDATING_SERVICE, service.getName());
        serviceUpdater.updateServiceParameters(client, service.getName(), service.getCredentials());
        getStepLogger().debug(Messages.SERVICE_UPDATED, service.getName());
    }

    private void deleteService(DelegateExecution context, CloudFoundryOperations client, CloudServiceExtended service) throws SLException {

        List<CloudApplicationExtended> apps = StepsUtil.getAppsToDeploy(context);
        List<CloudApplication> appsToUndeploy = StepsUtil.getAppsToUndeploy(context);

        // Find all apps in the current model that are bound to the service
        CloudServiceInstance esi = client.getServiceInstance(service.getName());
        List<CloudServiceBinding> bindings = esi.getBindings();
        List<String> boundAppNames = getBoundAppNames(client, apps, appsToUndeploy, bindings);
        getStepLogger().debug("Bound applications: " + boundAppNames);

        // Check if there are apps outside the current model that are bound to the service
        if (bindings.size() > boundAppNames.size()) {
            throw new SLException(Messages.CANT_DELETE_SERVICE, service.getName());
        }

        // Unbind bound apps from the service and finally delete the service
        getStepLogger().info(Messages.DELETING_SERVICE, service.getName());
        boundAppNames.forEach(appName -> client.unbindService(appName, service.getName()));
        client.deleteService(service.getName());
        getStepLogger().debug(Messages.SERVICE_DELETED, service.getName());
    }

    private void setServiceParameters(DelegateExecution context, CloudServiceExtended service, final String appArchiveId,
        final String fileName) throws FileStorageException, SLException {
        FileContentProcessor parametersFileProcessor = new FileContentProcessor() {
            @Override
            public void processFileContent(InputStream appArchiveStream) throws SLException {
                try (InputStream is = ArchiveHandler.getInputStream(appArchiveStream, fileName)) {
                    mergeCredentials(service, is);
                } catch (IOException e) {
                    throw new SLException(e, Messages.ERROR_RETRIEVING_MTA_RESOURCE_CONTENT, fileName);
                }
            }
        };
        fileService.processFileContent(
            new DefaultFileDownloadProcessor(StepsUtil.getSpaceId(context), appArchiveId, parametersFileProcessor));
    }

    private void mergeCredentials(CloudServiceExtended service, InputStream credentialsJson) throws SLException {
        Map<String, Object> existingCredentials = service.getCredentials();
        Map<String, Object> credentials = JsonUtil.convertJsonToMap(credentialsJson);
        if (existingCredentials == null) {
            existingCredentials = Collections.emptyMap();
        }
        Map<String, Object> result = PropertiesUtil.mergeExtensionProperties(credentials, existingCredentials);
        service.setCredentials(result);
    }

    private boolean shouldRecreate(CloudServiceExtended service, CloudService existingService) {
        boolean haveDifferentTypes = service.isUserProvided() ^ existingService.isUserProvided();
        boolean haveDifferentLabels = !Objects.equals(service.getLabel(), existingService.getLabel());
        return haveDifferentTypes || haveDifferentLabels;
    }

    private boolean shouldUpdateTags(CloudServiceExtended service, CloudService existingService, List<String> defaultTags) {
        if (!(existingService instanceof CloudServiceExtended)) {
            return false;
        }
        CloudServiceExtended existingServiceExtended = (CloudServiceExtended) existingService;
        Set<String> existingTags = new HashSet<>(existingServiceExtended.getTags());
        Set<String> newTags = new HashSet<>(service.getTags());
        if (defaultTags != null) {
            newTags.addAll(defaultTags);
        }

        return !existingTags.equals(newTags);
    }

    private boolean shouldUpdateCredentials(CloudServiceExtended service, Map<String, Object> credentials) {
        return !Objects.equals(service.getCredentials(), credentials);
    }

    private List<String> getBoundAppNames(CloudFoundryOperations client, List<CloudApplicationExtended> apps,
        List<CloudApplication> appsToUndeploy, List<CloudServiceBinding> bindings) {
        Set<String> appNames = apps.stream().map(app -> app.getName()).collect(Collectors.toSet());
        appNames.addAll(appsToUndeploy.stream().map((app) -> app.getName()).collect(Collectors.toSet()));

        List<CloudApplication> existingApps = client.getApplications();
        return bindings.stream().map(binding -> getApplication(client, binding, existingApps).getName()).filter(
            boundApp -> appNames.contains(boundApp)).collect(Collectors.toList());
    }

    private CloudApplication getApplication(CloudFoundryOperations client, CloudServiceBinding binding, List<CloudApplication> apps) {
        return apps.stream().filter(app -> app.getMeta().getGuid().equals(binding.getAppGuid())).findFirst().get();
    }

    private enum ServiceAction {
        ACTION_UPDATE_CREDENTIALS, ACTION_RECREATE, ACTION_UPDATE_TAGS, ACTION_UPDATE_SERVICE_PLAN
    }

    @Override
    protected String getIndexVariable() {
        return Constants.VAR_SERVICES_TO_CREATE_COUNT;
    }

    @Override
    public String getLogicalStepName() {
        return CreateOrUpdateServicesStep.class.getSimpleName();
    }

}
