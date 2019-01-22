package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.ServiceKey;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.XsCloudControllerClient;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceOfferingExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceGetter;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperation;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationState;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.process.analytics.model.ServiceAction;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.util.CommonUtil;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Component("determineServiceCreateUpdateActionsStep")
public class DetermineServiceCreateUpdateServiceActionsStep extends SyncFlowableStep {

    private static final String LAST_SERVICE_OPERATION = "last_operation";
    private static final String SERVICE_OPERATION_TYPE = "type";
    private static final String SERVICE_OPERATION_STATE = "state";

    @Inject
    private ServiceGetter serviceInstanceGetter;

    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws Exception {
        CloudControllerClient controllerClient = execution.getControllerClient();
        String spaceId = StepsUtil.getSpaceId(execution.getContext());
        CloudServiceExtended serviceToProcess = StepsUtil.getServiceToProcess(execution.getContext());

        execution.getStepLogger()
            .info(Messages.PROCESSING_SERVICE, serviceToProcess.getName());
        CloudService existingService = controllerClient.getService(serviceToProcess.getName(), false);

        Map<String, List<String>> defaultTags = computeDefaultTags(controllerClient);
        List<String> serviceDefaultTags = defaultTags.getOrDefault(serviceToProcess.getLabel(), Collections.emptyList());

        Map<String, List<ServiceKey>> serviceKeys = StepsUtil.getServiceKeysToCreate(execution.getContext());

        List<ServiceAction> actions = determineActions(controllerClient, spaceId, serviceToProcess, existingService, serviceDefaultTags,
            serviceKeys, execution);
        StepsUtil.setServiceActionsToExecute(actions, execution.getContext());
        StepsUtil.isServiceUpdated(false, execution.getContext());
        StepsUtil.setServiceToProcessName(serviceToProcess.getName(), execution.getContext());
        return StepPhase.DONE;
    }

    private List<ServiceAction> determineActions(CloudControllerClient client, String spaceId, CloudServiceExtended service,
        CloudService existingService, List<String> defaultTags, Map<String, List<ServiceKey>> serviceKeys, ExecutionWrapper execution) {
        List<ServiceAction> actions = new ArrayList<>();

        List<ServiceKey> keys = serviceKeys.get(service.getName());
        if (shouldUpdateKeys(service, keys, execution)) {
            getStepLogger().debug("Service keys should be updated");
            actions.add(ServiceAction.UPDATE_KEYS);
        }

        Map<String, Object> serviceInstanceEntity = serviceInstanceGetter.getServiceInstanceEntity(client, service.getName(), spaceId);
        // Check if the existing service should be updated or not
        if (shouldRecreate(service, existingService, serviceInstanceEntity, execution)) {
            getStepLogger().debug("Service should be recreated");
            getStepLogger().debug("New service: " + secureSerializer.toJson(service));
            getStepLogger().debug("Existing service: " + secureSerializer.toJson(existingService));
            StepsUtil.setServicesToDelete(execution.getContext(), Arrays.asList(service.getName()));
            actions.add(ServiceAction.RECREATE);
            return actions;
        }

        if (existingService == null) {
            getStepLogger().debug("Service should be created");
            getStepLogger().debug("New service: " + secureSerializer.toJson(service));
            actions.add(ServiceAction.CREATE);
            StepsUtil.setServicesToCreate(execution.getContext(), Arrays.asList(service));
            return actions;
        }

        if (shouldUpdatePlan(service, existingService)) {
            getStepLogger().debug("Service plan should be updated");
            getStepLogger().debug(MessageFormat.format("New service plan: {0}", service.getPlan()));
            getStepLogger().debug(MessageFormat.format("Existing service plan: {0}", existingService.getPlan()));
            actions.add(ServiceAction.UPDATE_PLAN);
        }

        List<String> existingServiceTags = getServiceTags(client, spaceId, existingService);
        if (shouldUpdateTags(client, spaceId, service, existingService, defaultTags, existingServiceTags)) {
            getStepLogger().debug("Service tags should be updated");
            getStepLogger().debug("New service tags: " + JsonUtil.toJson(service.getTags()));
            getStepLogger().debug("Existing service tags: " + JsonUtil.toJson(existingServiceTags));
            actions.add(ServiceAction.UPDATE_TAGS);
        }

        if (existingService != null) {
            CloudServiceInstance existingServiceInstance = client.getServiceInstance(service.getName(), false);
            if (existingServiceInstance != null && shouldUpdateCredentials(service, existingServiceInstance.getCredentials())) {
                getStepLogger().debug("Service parameters should be updated");
                getStepLogger().debug("New parameters: " + secureSerializer.toJson(service.getCredentials()));
                getStepLogger().debug("Existing service parameters: " + secureSerializer.toJson(existingServiceInstance.getCredentials()));
                actions.add(ServiceAction.UPDATE_CREDENTIALS);
            }
        }

        return actions;
    }

    private List<String> getServiceTags(CloudControllerClient client, String spaceId, CloudService service) {
        if (service instanceof CloudServiceExtended) {
            CloudServiceExtended serviceExtended = (CloudServiceExtended) service;
            return serviceExtended.getTags();
        }
        Map<String, Object> serviceInstance = serviceInstanceGetter.getServiceInstanceEntity(client, service.getName(), spaceId);
        return CommonUtil.cast(serviceInstance.get("tags"));
    }

    private boolean shouldUpdateKeys(CloudServiceExtended service, List<ServiceKey> list, ExecutionWrapper execution) {
        return service.isUserProvided() || list == null || list.isEmpty() ? false : true;
    }

    private boolean shouldUpdatePlan(CloudServiceExtended service, CloudService existingService) {
        return !Objects.equals(service.getPlan(), existingService.getPlan());
    }

    private boolean shouldRecreate(CloudServiceExtended service, CloudService existingService, Map<String, Object> serviceInstanceEntity,
        ExecutionWrapper execution) {
        if(serviceInstanceEntity == null) {
            return false;
        }
        ServiceOperation lastOperation = getLastOperation(execution, serviceInstanceEntity);
        if (lastOperation != null && lastOperation.getType() == ServiceOperationType.CREATE
            && lastOperation.getState() == ServiceOperationState.FAILED) {
            return true;
        }
        if (!StepsUtil.shouldDeleteServices(execution.getContext())) {
            return false;
        }
        boolean haveDifferentTypes = service.isUserProvided() ^ existingService.isUserProvided();
        if (existingService.isUserProvided()) {
            return haveDifferentTypes;
        }
        boolean haveDifferentLabels = !Objects.equals(service.getLabel(), existingService.getLabel());
        return haveDifferentTypes || haveDifferentLabels;
    }

    private boolean shouldUpdateTags(CloudControllerClient client, String spaceId, CloudServiceExtended service,
        CloudService existingService, List<String> defaultTags, List<String> existingServiceTags) {
        if (service.isUserProvided()) {
            return false;
        }
        List<String> existingTags = existingServiceTags;
        List<String> newServiceTags = new ArrayList<>(service.getTags());
        existingTags.removeAll(defaultTags);
        newServiceTags.removeAll(defaultTags);
        return !existingTags.equals(newServiceTags);
    }

    private boolean shouldUpdateCredentials(CloudServiceExtended service, Map<String, Object> credentials) {
        return !Objects.equals(service.getCredentials(), credentials);
    }

    private List<String> getServiceTags(Map<String, Object> serviceInstanceEntity) {
        if (serviceInstanceEntity == null) {
            return Collections.emptyList();
        }
        return CommonUtil.cast(serviceInstanceEntity.getOrDefault("tags", Collections.emptyList()));
    }

    private Map<String, List<String>> computeDefaultTags(CloudControllerClient client) {
        if (!(client instanceof XsCloudControllerClient)) {
            return Collections.emptyMap();
        }

        XsCloudControllerClient xsClient = (XsCloudControllerClient) client;
        Map<String, List<String>> defaultTags = new HashMap<>();
        for (CloudServiceOfferingExtended serviceOffering : xsClient.getExtendedServiceOfferings()) {
            defaultTags.put(serviceOffering.getLabel(), serviceOffering.getTags());
        }
        return defaultTags;
    }

    private ServiceOperation getLastOperation(ExecutionWrapper execution, Map<String, Object> cloudServiceInstance) {
        Map<String, Object> lastOperationAsMap = (Map<String, Object>) cloudServiceInstance.get(LAST_SERVICE_OPERATION);
        if (lastOperationAsMap == null) {
            return null;
        }
        ServiceOperation lastOperation = parseServiceOperationFromMap(lastOperationAsMap);
        return lastOperation;
    }

    private ServiceOperation parseServiceOperationFromMap(Map<String, Object> serviceOperation) {
        if (serviceOperation.get(SERVICE_OPERATION_TYPE) == null || serviceOperation.get(SERVICE_OPERATION_STATE) == null) {
            return null;
        }
        ServiceOperationType type = ServiceOperationType.fromString((String) serviceOperation.get(SERVICE_OPERATION_TYPE));
        ServiceOperationState state = ServiceOperationState.fromString((String) serviceOperation.get(SERVICE_OPERATION_STATE));
        return new ServiceOperation(type, null, state);
    }
}
