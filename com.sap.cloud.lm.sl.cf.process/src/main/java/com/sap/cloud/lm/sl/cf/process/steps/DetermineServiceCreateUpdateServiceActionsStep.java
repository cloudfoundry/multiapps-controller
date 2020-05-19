package com.sap.cloud.lm.sl.cf.process.steps;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.CloudServiceKey;
import org.cloudfoundry.client.v3.Metadata;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceInstanceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceGetter;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ResourceType;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaArchiveElements;
import com.sap.cloud.lm.sl.cf.core.model.ServiceOperation;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.persistence.services.FileContentProcessor;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ServiceAction;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.MiscUtil;
import com.sap.cloud.lm.sl.mta.handlers.ArchiveHandler;
import com.sap.cloud.lm.sl.mta.util.PropertiesUtil;

@Named("determineServiceCreateUpdateActionsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DetermineServiceCreateUpdateServiceActionsStep extends SyncFlowableStep {

    @Inject
    private ServiceGetter serviceInstanceGetter;

    private final SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Override
    protected StepPhase executeStep(ProcessContext context) throws Exception {
        CloudControllerClient controllerClient = context.getControllerClient();
        String spaceId = context.getVariable(Variables.SPACE_GUID);
        CloudServiceInstanceExtended serviceToProcess = context.getVariable(Variables.SERVICE_TO_PROCESS);

        context.getStepLogger()
               .info(Messages.PROCESSING_SERVICE, serviceToProcess.getName());
        CloudServiceInstance existingService = controllerClient.getServiceInstance(serviceToProcess.getName(), false);

        Map<String, List<CloudServiceKey>> serviceKeys = context.getVariable(Variables.SERVICE_KEYS_TO_CREATE);

        setServiceParameters(context, serviceToProcess);

        serviceToProcess = context.getVariable(Variables.SERVICE_TO_PROCESS);

        List<ServiceAction> actions = determineActionsAndHandleExceptions(controllerClient, spaceId, serviceToProcess, existingService,
                                                                          serviceKeys, context);

        context.setVariable(Variables.SERVICE_ACTIONS_TO_EXCECUTE, actions);
        context.setVariable(Variables.IS_SERVICE_UPDATED, false);
        context.setVariable(Variables.SERVICE_TO_PROCESS_NAME, serviceToProcess.getName());
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return MessageFormat.format(Messages.ERROR_DETERMINING_ACTIONS_TO_EXECUTE_ON_SERVICE,
                                    context.getVariable(Variables.SERVICE_TO_PROCESS)
                                           .getName());
    }

    private List<ServiceAction>
            determineActionsAndHandleExceptions(CloudControllerClient client, String spaceId, CloudServiceInstanceExtended service,
                                                CloudServiceInstance existingService, Map<String, List<CloudServiceKey>> serviceKeys,
                                                ProcessContext context) {
        try {
            return determineActions(client, spaceId, service, existingService, serviceKeys, context);
        } catch (CloudOperationException e) {
            String determineServiceActionsFailedMessage = MessageFormat.format(Messages.ERROR_DETERMINING_ACTIONS_TO_EXECUTE_ON_SERVICE,
                                                                               service.getName(), e.getStatusText());
            throw new CloudOperationException(e.getStatusCode(), determineServiceActionsFailedMessage, e.getDescription(), e);
        }
    }

    private void setServiceParameters(ProcessContext context, CloudServiceInstanceExtended service) throws FileStorageException {
        service = prepareServiceParameters(context, service);
        context.setVariable(Variables.SERVICE_TO_PROCESS, service);
    }

    private List<ServiceAction> determineActions(CloudControllerClient client, String spaceId, CloudServiceInstanceExtended service,
                                                 CloudServiceInstance existingService, Map<String, List<CloudServiceKey>> serviceKeys,
                                                 ProcessContext context) {
        List<ServiceAction> actions = new ArrayList<>();

        List<CloudServiceKey> keys = serviceKeys.get(service.getResourceName());
        if (shouldUpdateKeys(service, keys)) {
            getStepLogger().debug("Service keys should be updated");
            actions.add(ServiceAction.UPDATE_KEYS);
        }

        if (existingService == null) {
            getStepLogger().debug("Service should be created");
            actions.add(ServiceAction.CREATE);
            context.setVariable(Variables.SERVICES_TO_CREATE, Collections.singletonList(service));
            return actions;
        }
        getStepLogger().debug("Existing service: " + secureSerializer.toJson(existingService));

        boolean shouldRecreate = false;
        if (haveDifferentTypesOrLabels(service, existingService)) {
            if (context.getVariable(Variables.DELETE_SERVICES)) {
                shouldRecreate = true;
            } else {
                throw getServiceRecreationNeededException(service, existingService);
            }
        }
        ServiceOperation lastOperation = getLastOperation(client, spaceId, existingService.getName());
        if (isInDangerousState(lastOperation)) {
            if (context.getVariable(Variables.DELETE_SERVICES)) {
                shouldRecreate = true;
            } else {
                getStepLogger().warn(Messages.SERVICE_0_IS_IN_STATE_1_AND_MAY_NOT_BE_OPERATIONAL, existingService.getName(), lastOperation);
            }
        }
        if (shouldRecreate) {
            getStepLogger().debug("Service should be recreated");
            context.setVariable(Variables.SERVICE_TO_DELETE, service.getName());
            actions.add(ServiceAction.RECREATE);
            return actions;
        }

        if (shouldUpdatePlan(service, existingService)) {
            getStepLogger().debug("Service plan should be updated");
            getStepLogger().debug(MessageFormat.format("New service plan: {0}", service.getPlan()));
            getStepLogger().debug(MessageFormat.format("Existing service plan: {0}", existingService.getPlan()));
            actions.add(ServiceAction.UPDATE_PLAN);
        }

        List<String> existingServiceTags = getServiceTags(client, spaceId, existingService);
        if (shouldUpdateTags(service, existingServiceTags)) {
            getStepLogger().debug("Service tags should be updated");
            getStepLogger().debug("New service tags: " + JsonUtil.toJson(service.getTags()));
            getStepLogger().debug("Existing service tags: " + JsonUtil.toJson(existingServiceTags));
            actions.add(ServiceAction.UPDATE_TAGS);
        }

        if (shouldUpdateCredentials(service, existingService, client)) {
            getStepLogger().debug("Service parameters should be updated");
            getStepLogger().debug("New parameters: " + secureSerializer.toJson(service.getCredentials()));
            actions.add(ServiceAction.UPDATE_CREDENTIALS);
        }

        if (shouldUpdateMetadata(service, existingService)) {
            getStepLogger().debug("Service metadata should be updated");
            getStepLogger().debug("New metadata: " + secureSerializer.toJson(service.getV3Metadata()));
            actions.add(ServiceAction.UPDATE_METADATA);
        }

        return actions;
    }

    private ServiceOperation getLastOperation(CloudControllerClient client, String spaceId, String serviceName) {
        Map<String, Object> existingServiceInstanceEntity = serviceInstanceGetter.getServiceInstanceEntity(client, serviceName, spaceId);
        if (existingServiceInstanceEntity != null) {
            return getLastOperation(existingServiceInstanceEntity);
        }
        return null;
    }

    private SLException getServiceRecreationNeededException(CloudServiceInstanceExtended service, CloudServiceInstance existingService) {
        return new SLException(Messages.ERROR_SERVICE_NEEDS_TO_BE_RECREATED_BUT_FLAG_NOT_SET,
                               service.getResourceName(),
                               buildServiceType(service),
                               existingService.getName(),
                               buildServiceType(existingService));
    }

    private String buildServiceType(CloudServiceInstance service) {
        if (service.isUserProvided()) {
            return ResourceType.USER_PROVIDED_SERVICE.toString();
        }

        String label = ObjectUtils.isEmpty(service.getLabel()) ? "unknown label" : service.getLabel();
        String plan = ObjectUtils.isEmpty(service.getPlan()) ? "unknown plan" : service.getPlan();
        return label + "/" + plan;
    }

    private boolean shouldUpdateMetadata(CloudServiceInstanceExtended service, CloudServiceInstance existingService) {
        Metadata existingMetadata = existingService.getV3Metadata();
        Metadata newMetadata = service.getV3Metadata();
        if (existingMetadata != null && newMetadata != null) {
            return !existingMetadata.equals(newMetadata);
        }
        return newMetadata != null;
    }

    private CloudServiceInstanceExtended prepareServiceParameters(ProcessContext context, CloudServiceInstanceExtended service)
        throws FileStorageException {
        MtaArchiveElements mtaArchiveElements = context.getVariable(Variables.MTA_ARCHIVE_ELEMENTS);
        String fileName = mtaArchiveElements.getResourceFileName(service.getResourceName());
        if (fileName != null) {
            getStepLogger().info(Messages.SETTING_SERVICE_PARAMETERS, service.getName(), fileName);
            String appArchiveId = context.getRequiredVariable(Variables.APP_ARCHIVE_ID);
            return setServiceParameters(context, service, appArchiveId, fileName);
        }
        return service;
    }

    private CloudServiceInstanceExtended setServiceParameters(ProcessContext context, CloudServiceInstanceExtended service,
                                                              String appArchiveId, String fileName)
        throws FileStorageException {
        FileContentProcessor<CloudServiceInstanceExtended> parametersFileProcessor = appArchiveStream -> {
            try (InputStream is = ArchiveHandler.getInputStream(appArchiveStream, fileName, configuration.getMaxManifestSize())) {
                return mergeCredentials(service, is);
            } catch (IOException e) {
                throw new SLException(e, Messages.ERROR_RETRIEVING_MTA_RESOURCE_CONTENT, fileName);
            }
        };
        return fileService.processFileContent(context.getVariable(Variables.SPACE_GUID), appArchiveId, parametersFileProcessor);
    }

    private CloudServiceInstanceExtended mergeCredentials(CloudServiceInstanceExtended service, InputStream credentialsJson) {
        Map<String, Object> existingCredentials = service.getCredentials();
        Map<String, Object> credentials = JsonUtil.convertJsonToMap(credentialsJson);
        if (existingCredentials == null) {
            existingCredentials = Collections.emptyMap();
        }
        Map<String, Object> result = PropertiesUtil.mergeExtensionProperties(credentials, existingCredentials);
        return ImmutableCloudServiceInstanceExtended.copyOf(service)
                                                    .withCredentials(result);
    }

    private List<String> getServiceTags(CloudControllerClient client, String spaceId, CloudServiceInstance service) {
        if (service instanceof CloudServiceInstanceExtended) {
            CloudServiceInstanceExtended serviceExtended = (CloudServiceInstanceExtended) service;
            return serviceExtended.getTags();
        }
        Map<String, Object> serviceInstance = serviceInstanceGetter.getServiceInstanceEntity(client, service.getName(), spaceId);
        return MiscUtil.cast(serviceInstance.get("tags"));
    }

    private boolean shouldUpdateKeys(CloudServiceInstanceExtended service, List<CloudServiceKey> serviceKeys) {
        return !(service.isUserProvided() || CollectionUtils.isEmpty(serviceKeys));
    }

    private boolean shouldUpdatePlan(CloudServiceInstanceExtended service, CloudServiceInstance existingService) {
        return !Objects.equals(service.getPlan(), existingService.getPlan());
    }

    private boolean isInDangerousState(ServiceOperation lastOperation) {
        if (lastOperation == null) {
            return false;
        }
        return hasType(lastOperation, ServiceOperation.Type.CREATE, ServiceOperation.Type.DELETE) && hasFailed(lastOperation);
    }

    private boolean hasType(ServiceOperation serviceOperation, ServiceOperation.Type... types) {
        return Arrays.stream(types)
                     .anyMatch(type -> serviceOperation.getType() == type);
    }

    private boolean hasFailed(ServiceOperation serviceOperation) {
        return serviceOperation.getState() == ServiceOperation.State.FAILED;
    }

    private boolean haveDifferentTypesOrLabels(CloudServiceInstanceExtended service, CloudServiceInstance existingService) {
        boolean haveDifferentTypes = service.isUserProvided() ^ existingService.isUserProvided();
        if (existingService.isUserProvided()) {
            return haveDifferentTypes;
        }
        boolean haveDifferentLabels = !Objects.equals(service.getLabel(), existingService.getLabel());
        return haveDifferentTypes || haveDifferentLabels;
    }

    private boolean shouldUpdateTags(CloudServiceInstanceExtended service, List<String> existingServiceTags) {
        if (service.isUserProvided()) {
            return false;
        }
        existingServiceTags = ObjectUtils.defaultIfNull(existingServiceTags, Collections.emptyList());
        return !existingServiceTags.equals(service.getTags());
    }

    private boolean shouldUpdateCredentials(CloudServiceInstanceExtended service, CloudServiceInstance existingService,
                                            CloudControllerClient client) {
        try {
            Map<String, Object> serviceParameters = client.getServiceInstanceParameters(existingService.getMetadata()
                                                                                                       .getGuid());
            getStepLogger().debug("Existing service parameters: " + secureSerializer.toJson(serviceParameters));
            return !Objects.equals(service.getCredentials(), serviceParameters);
        } catch (CloudOperationException e) {
            if (HttpStatus.NOT_IMPLEMENTED == e.getStatusCode() || HttpStatus.BAD_REQUEST == e.getStatusCode()) {
                getStepLogger().warnWithoutProgressMessage(Messages.CANNOT_RETRIEVE_SERVICE_INSTANCE_PARAMETERS, service.getName());
                // TODO: Optimization (Hack) that should be deprecated at some point. So here is a todo for that.
                return !MapUtils.isEmpty(service.getCredentials());
            }
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private ServiceOperation getLastOperation(Map<String, Object> cloudServiceInstance) {
        Map<String, Object> lastOperationAsMap = (Map<String, Object>) cloudServiceInstance.get(ServiceOperation.LAST_SERVICE_OPERATION);
        if (lastOperationAsMap == null) {
            return null;
        }
        return parseServiceOperationFromMap(lastOperationAsMap);
    }

    private ServiceOperation parseServiceOperationFromMap(Map<String, Object> serviceOperation) {
        if (serviceOperation.get(ServiceOperation.SERVICE_OPERATION_TYPE) == null
            || serviceOperation.get(ServiceOperation.SERVICE_OPERATION_STATE) == null) {
            return null;
        }
        return ServiceOperation.fromMap(serviceOperation);
    }
}
