package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.io.IOException;
import java.io.InputStream;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ServiceKey;
import com.sap.cloud.lm.sl.cf.core.cf.clients.DefaultTagsDetector;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceCreator;
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
public class CreateOrUpdateServicesStep extends AbstractXS2ProcessStepWithBridge {

    static final Logger LOGGER = LoggerFactory.getLogger(CreateOrUpdateServicesStep.class);

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

    @Override
    protected ExecutionStatus pollStatusInternal(DelegateExecution context) throws SLException, FileStorageException {

        logActivitiTask(context, LOGGER);
        try {
            info(context, Messages.CREATING_OR_UPDATING_SERVICES, LOGGER);

            CloudFoundryOperations client = getCloudFoundryClient(context, LOGGER);
            Map<String, List<String>> defaultTags = defaultTagsDetector.computeDefaultTags(client);
            debug(context, "Default tags: " + JsonUtil.toJson(defaultTags, true), LOGGER);

            List<CloudService> existingServices = client.getServices();
            Map<String, CloudService> existingServicesMap = getServicesMap(existingServices);
            debug(context, "Existing services: " + existingServicesMap.keySet(), LOGGER);

            List<CloudServiceExtended> services = StepsUtil.getServicesToCreate(context);
            Map<String, List<ServiceKey>> serviceKeys = StepsUtil.getServiceKeysToCreate(context);

            Map<String, ServiceOperationType> triggeredServiceOperations = createOrUpdateServices(context, client, services,
                existingServicesMap, serviceKeys, defaultTags);
            StepsUtil.setTriggeredServiceOperations(context, triggeredServiceOperations);

            debug(context, Messages.SERVICES_CREATED_OR_UPDATED, LOGGER);
            return ExecutionStatus.SUCCESS;
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            error(context, Messages.ERROR_CREATING_SERVICES, e, LOGGER);
            throw e;
        } catch (SLException e) {
            error(context, Messages.ERROR_CREATING_SERVICES, e, LOGGER);
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
            () -> clientExtensions.getServiceKeys(service.getName()));

        if (existingServiceKeys == null) {
            return;
        }

        List<ServiceKey> serviceKeysToCreate = getServiceKeysToCreate(serviceKeys, existingServiceKeys);
        List<ServiceKey> serviceKeysToUpdate = getServiceKeysToUpdate(serviceKeys, existingServiceKeys);
        List<ServiceKey> serviceKeysToDelete = getServiceKeysToDelete(serviceKeys, existingServiceKeys);

        if (canDeleteServiceKeys(context)) {
            deleteServiceKeys(context, clientExtensions, serviceKeysToDelete);
            // Recreate the service keys, which should be updated, as direct update is not supported
            // by the controller:
            deleteServiceKeys(context, clientExtensions, serviceKeysToUpdate);
            createServiceKeys(context, clientExtensions, serviceKeysToUpdate);
        } else {
            serviceKeysToDelete.forEach((key) -> {
                warn(context, format(Messages.WILL_NOT_DELETE_SERVICE_KEY, key.getName(), key.getService().getName()), LOGGER);
            });
            serviceKeysToUpdate.forEach((key) -> {
                warn(context, format(Messages.WILL_NOT_UPDATE_SERVICE_KEY, key.getName(), key.getService().getName()), LOGGER);
            });
        }
        createServiceKeys(context, clientExtensions, serviceKeysToCreate);
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

    private void deleteServiceKeys(DelegateExecution context, ClientExtensions client, List<ServiceKey> serviceKeys) {
        serviceKeys.stream().forEach(key -> deleteServiceKey(context, client, key));
    }

    private void createServiceKeys(DelegateExecution context, ClientExtensions client, List<ServiceKey> serviceKeys) {
        serviceKeys.stream().forEach(key -> createServiceKey(context, client, key));
    }

    private void createServiceKey(DelegateExecution context, ClientExtensions client, ServiceKey key) {
        info(context, format(Messages.CREATING_SERVICE_KEY_FOR_SERVICE, key.getName(), key.getService().getName()), LOGGER);
        client.createServiceKey(key.getService().getName(), key.getName(), JsonUtil.toJson(key.getParameters()));
    }

    private void deleteServiceKey(DelegateExecution context, ClientExtensions client, ServiceKey key) {
        info(context, format(Messages.DELETING_SERVICE_KEY_FOR_SERVICE, key.getName(), key.getService().getName()), LOGGER);
        client.deleteServiceKey(key.getService().getName(), key.getName());
    }

    private ServiceOperationType createOrUpdateService(DelegateExecution context, CloudFoundryOperations client,
        CloudServiceExtended service, CloudService existingService, List<String> defaultTags) throws SLException, FileStorageException {

        // Set service parameters if a file containing their values exists:
        String fileName = StepsUtil.getResourceFileName(context, service.getResourceName());
        if (fileName != null) {
            debug(context, format(Messages.SETTING_SERVICE_PARAMETERS, service.getName(), fileName), LOGGER);
            String appArchiveId = StepsUtil.getRequiredStringParameter(context, Constants.PARAM_APP_ARCHIVE_ID);
            setServiceParameters(context, service, appArchiveId, fileName);
        }

        if (existingService == null) {
            serviceOperationExecutor.executeServiceOperation(service, () -> createService(context, client, service));
            return ServiceOperationType.CREATE;
        }

        debug(context, format(Messages.SERVICE_ALREADY_EXISTS, service.getName()), LOGGER);
        List<ServiceAction> actions = determineActions(context, client, service, existingService, defaultTags);
        if (actions.contains(ServiceAction.ACTION_RECREATE)) {
            boolean deleteAllowed = (boolean) context.getVariable(Constants.PARAM_DELETE_SERVICES);
            if (!deleteAllowed) {
                warn(context, format(Messages.WILL_NOT_RECREATE_SERVICE, service.getName()), LOGGER);
                return null;
            }
            serviceOperationExecutor.executeServiceOperation(service, () -> deleteService(context, client, service));
            serviceOperationExecutor.executeServiceOperation(service, () -> createService(context, client, service));
            return ServiceOperationType.UPDATE;
        }
        ServiceOperationType type = null;
        if (actions.contains(ServiceAction.ACTION_UPDATE_CREDENTIALS)) {
            serviceOperationExecutor.executeServiceOperation(service, () -> updateServiceCredentials(context, client, service));
            type = ServiceOperationType.UPDATE;
        }
        if (actions.contains(ServiceAction.ACTION_UPDATE_TAGS)) {
            serviceOperationExecutor.executeServiceOperation(service, () -> updateServiceTags(context, client, service));
            type = ServiceOperationType.UPDATE;
        }
        if (actions.isEmpty()) {
            info(context, format(Messages.SERVICE_UNCHANGED, existingService.getName()), LOGGER);
        }
        return type;
    }

    private List<ServiceAction> determineActions(DelegateExecution context, CloudFoundryOperations client, CloudServiceExtended service,
        CloudService existingService, List<String> defaultTags) {
        List<ServiceAction> actions = new ArrayList<>();

        debug(context, "Determining action to be performed on existing service...", LOGGER);

        // Check if the existing service should be updated or not
        if (shouldRecreate(service, existingService)) {
            debug(context, "Service should be recreated", LOGGER);
            debug(context, "New service: " + secureSerializer.toJson(service), LOGGER);
            debug(context, "Existing service: " + secureSerializer.toJson(existingService), LOGGER);
            return Arrays.asList(ServiceAction.ACTION_RECREATE);
        }
        if (!existingService.isUserProvided()) {
            CloudServiceInstance existingServiceInstance = client.getServiceInstance(service.getName());
            if (existingServiceInstance != null && shouldUpdateCredentials(service, existingServiceInstance.getCredentials())) {
                debug(context, "Service parameters should be updated", LOGGER);
                debug(context, "New parameters: " + secureSerializer.toJson(service.getCredentials()), LOGGER);
                debug(context, "Existing service parameters: " + secureSerializer.toJson(existingServiceInstance.getCredentials()), LOGGER);
                actions.add(ServiceAction.ACTION_UPDATE_CREDENTIALS);
            }
        } else if (existingService.isUserProvided()) {
            CloudServiceInstance esi = client.getServiceInstance(service.getName());
            if (esi != null && shouldUpdateCredentials(service, esi.getCredentials())) {
                debug(context, "User-provided service credentials should be updated", LOGGER);
                debug(context, "New credentials: " + secureSerializer.toJson(service.getCredentials()), LOGGER);
                debug(context, "Existing service instance credentials: " + secureSerializer.toJson(esi.getCredentials()), LOGGER);
                actions.add(ServiceAction.ACTION_UPDATE_CREDENTIALS);
            }
        }
        if (shouldUpdateTags(service, existingService, defaultTags)) {
            CloudServiceExtended existingServiceExtended = (CloudServiceExtended) existingService;
            debug(context, "Service tags should be updated", LOGGER);
            debug(context, "New service tags: " + JsonUtil.toJson(service.getTags()), LOGGER);
            debug(context, "Existing service tags: " + JsonUtil.toJson(existingServiceExtended.getTags()), LOGGER);
            actions.add(ServiceAction.ACTION_UPDATE_TAGS);
        }

        if (actions.isEmpty()) {
            debug(context, "Nothing to do, unable to detect changes", LOGGER);
        }

        return actions;
    }

    private void createService(DelegateExecution context, CloudFoundryOperations client, CloudServiceExtended service) {
        info(context, format(Messages.CREATING_SERVICE, service.getName()), LOGGER);
        if (service.isUserProvided()) {
            client.createUserProvidedService(service, service.getCredentials());
        } else {
            serviceCreator.createService(client, service, StepsUtil.getSpaceId(context));
        }
        debug(context, format(Messages.SERVICE_CREATED, service.getName()), LOGGER);
    }

    private void updateServiceTags(DelegateExecution context, CloudFoundryOperations client, CloudServiceExtended service)
        throws SLException {
        ClientExtensions clientExtensions = getClientExtensions(context);
        // TODO: Remove the service.isUserProvided() check when user provided services support tags.
        // See the following issue for more info:
        // https://www.pivotaltracker.com/n/projects/966314/stories/105674948
        if (clientExtensions == null || service.isUserProvided())
            return;
        info(context, format(Messages.UPDATING_SERVICE_TAGS, service.getName()), LOGGER);
        clientExtensions.updateServiceTags(service.getName(), service.getTags());
        debug(context, format(Messages.SERVICE_TAGS_UPDATED, service.getName()), LOGGER);
    }

    private void updateServiceCredentials(DelegateExecution context, CloudFoundryOperations client, CloudServiceExtended service)
        throws SLException {
        ClientExtensions clientExtensions = getClientExtensions(context);
        info(context, format(Messages.UPDATING_SERVICE, service.getName()), LOGGER);
        if (clientExtensions == null) {
            serviceCreator.updateServiceParameters(client, service.getName(), service.getCredentials());
        } else {
            updateServiceCredentialsViaClientExtensions(service, clientExtensions);
        }
        debug(context, format(Messages.SERVICE_UPDATED, service.getName()), LOGGER);
    }

    private void updateServiceCredentialsViaClientExtensions(CloudServiceExtended service, ClientExtensions clientExtensions) {
        if (service.isUserProvided()) {
            clientExtensions.updateUserProvidedServiceCredentials(service.getName(), service.getCredentials());
        } else {
            clientExtensions.updateServiceParameters(service.getName(), service.getCredentials());
        }
    }

    private void deleteService(DelegateExecution context, CloudFoundryOperations client, CloudServiceExtended service) throws SLException {

        List<CloudApplicationExtended> apps = StepsUtil.getAppsToDeploy(context);
        List<CloudApplication> appsToUndeploy = StepsUtil.getAppsToUndeploy(context);

        // Find all apps in the current model that are bound to the service
        CloudServiceInstance esi = client.getServiceInstance(service.getName());
        List<CloudServiceBinding> bindings = esi.getBindings();
        List<String> boundAppNames = getBoundAppNames(client, apps, appsToUndeploy, bindings);
        debug(context, "Bound applications: " + boundAppNames, LOGGER);

        // Check if there are apps outside the current model that are bound to the service
        if (bindings.size() > boundAppNames.size()) {
            throw new SLException(Messages.CANT_DELETE_SERVICE, service.getName());
        }

        // Unbind bound apps from the service and finally delete the service
        info(context, format(Messages.DELETING_SERVICE, service.getName()), LOGGER);
        boundAppNames.forEach(appName -> client.unbindService(appName, service.getName()));
        client.deleteService(service.getName());
        debug(context, format(Messages.SERVICE_DELETED, service.getName()), LOGGER);
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
        boolean haveDifferentPlans = !Objects.equals(service.getPlan(), existingService.getPlan());
        boolean haveDifferentLabels = !Objects.equals(service.getLabel(), existingService.getLabel());
        return haveDifferentTypes || haveDifferentPlans || haveDifferentLabels;
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

    private ClientExtensions getClientExtensions(DelegateExecution context) throws SLException {
        ClientExtensions clientExtensions = getClientExtensions(context, LOGGER);
        return clientExtensions;
    }

    private enum ServiceAction {
        ACTION_UPDATE_CREDENTIALS, ACTION_RECREATE, ACTION_UPDATE_TAGS,
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
