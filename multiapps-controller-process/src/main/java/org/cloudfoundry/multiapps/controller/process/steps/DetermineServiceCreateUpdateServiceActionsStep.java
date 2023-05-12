package org.cloudfoundry.multiapps.controller.process.steps;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Named;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.cf.v2.ResourceType;
import org.cloudfoundry.multiapps.controller.core.helpers.MtaArchiveElements;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerialization;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ServiceAction;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.handlers.ArchiveHandler;
import org.cloudfoundry.multiapps.mta.util.PropertiesUtil;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ServiceOperation;

@Named("determineServiceCreateUpdateActionsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DetermineServiceCreateUpdateServiceActionsStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) throws Exception {
        CloudControllerClient client = context.getControllerClient();
        CloudServiceInstanceExtended serviceToProcess = context.getVariable(Variables.SERVICE_TO_PROCESS);
        getStepLogger().info(Messages.PROCESSING_SERVICE, serviceToProcess.getName());
        CloudServiceInstance existingService = client.getServiceInstance(serviceToProcess.getName(), false);
        setServiceParameters(context, serviceToProcess);

        List<ServiceAction> actions = determineActionsAndHandleExceptions(context, existingService);

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

    private List<ServiceAction> determineActionsAndHandleExceptions(ProcessContext context, CloudServiceInstance existingService) {
        CloudServiceInstanceExtended service = context.getVariable(Variables.SERVICE_TO_PROCESS);
        try {
            return determineActions(context, service, existingService);
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

    private List<ServiceAction> determineActions(ProcessContext context, CloudServiceInstanceExtended service,
                                                 CloudServiceInstance existingService) {
        List<ServiceAction> actions = new ArrayList<>();
        if (shouldUpdateKeys(service, existingService, context)) {
            getStepLogger().debug("Service keys should be updated");
            actions.add(ServiceAction.UPDATE_KEYS);
        }

        if (existingService == null) {
            getStepLogger().debug("Service should be created");
            actions.add(ServiceAction.CREATE);
            context.setVariable(Variables.SERVICES_TO_CREATE, Collections.singletonList(service));
            return actions;
        }
        getStepLogger().debug("Existing service: " + SecureSerialization.toJson(existingService));

        boolean shouldRecreate = false;
        if (haveDifferentTypesOrLabels(service, existingService)) {
            if (context.getVariable(Variables.DELETE_SERVICES)) {
                shouldRecreate = true;
            } else {
                throw getServiceRecreationNeededException(service, existingService);
            }
        }

        ServiceOperation lastOperation = existingService.getLastOperation();
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

        List<String> existingServiceTags = existingService.getTags();
        if (shouldUpdateTags(service, existingServiceTags)) {
            getStepLogger().debug("Service tags should be updated");
            getStepLogger().debug("New service tags: " + JsonUtil.toJson(service.getTags()));
            getStepLogger().debug("Existing service tags: " + JsonUtil.toJson(existingServiceTags));
            actions.add(ServiceAction.UPDATE_TAGS);
        }

        String existingSyslogDrainUrl = existingService.getSyslogDrainUrl();
        if (shouldUpdateSyslogUrl(service, existingSyslogDrainUrl)) {
            getStepLogger().debug("Syslog drain url should be updated");
            getStepLogger().debug("New syslog drain url: " + service.getSyslogDrainUrl());
            getStepLogger().debug("Existing syslog drain url: " + existingService.getSyslogDrainUrl());
            actions.add(ServiceAction.UPDATE_SYSLOG_URL);
        }

        CloudControllerClient client = context.getControllerClient();
        if (shouldUpdateCredentials(service, existingService, client)) {
            getStepLogger().debug("Service parameters should be updated");
            getStepLogger().debug("New parameters: " + SecureSerialization.toJson(service.getCredentials()));
            actions.add(ServiceAction.UPDATE_CREDENTIALS);
        }

        if (shouldUpdateMetadata(service, existingService)) {
            getStepLogger().debug("Service metadata should be updated");
            getStepLogger().debug("New metadata: " + SecureSerialization.toJson(service.getV3Metadata()));
            actions.add(ServiceAction.UPDATE_METADATA);
        }

        return actions;
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
            return setServiceParameters(context, service, fileName);
        }
        return service;
    }

    private CloudServiceInstanceExtended setServiceParameters(ProcessContext context, CloudServiceInstanceExtended service, String fileName)
        throws FileStorageException {
        String appArchiveId = context.getRequiredVariable(Variables.APP_ARCHIVE_ID);
        String spaceGuid = context.getVariable(Variables.SPACE_GUID);
        return fileService.processFileContent(spaceGuid, appArchiveId, appArchiveStream -> {
            try (InputStream is = ArchiveHandler.getInputStream(appArchiveStream, fileName, configuration.getMaxManifestSize())) {
                return mergeCredentials(service, is);
            } catch (IOException e) {
                throw new SLException(e, Messages.ERROR_RETRIEVING_MTA_RESOURCE_CONTENT, fileName);
            }
        });
    }

    private CloudServiceInstanceExtended mergeCredentials(CloudServiceInstanceExtended service, InputStream credentialsJson) {
        Map<String, Object> existingCredentials = ObjectUtils.defaultIfNull(service.getCredentials(), Collections.emptyMap());
        Map<String, Object> credentials = JsonUtil.convertJsonToMap(credentialsJson);
        Map<String, Object> result = PropertiesUtil.mergeExtensionProperties(credentials, existingCredentials);
        return ImmutableCloudServiceInstanceExtended.copyOf(service)
                                                    .withCredentials(result);
    }

    private boolean shouldUpdateKeys(CloudServiceInstanceExtended service, CloudServiceInstance existingService, ProcessContext context) {
        if (service.isUserProvided()) {
            return false;
        }
        return hasServiceKeysForCreation(context, service) || shouldDeleteServiceKeys(existingService, context);
    }

    private boolean hasServiceKeysForCreation(ProcessContext context, CloudServiceInstanceExtended service) {
        Map<String, List<CloudServiceKey>> serviceKeys = context.getVariable(Variables.SERVICE_KEYS_TO_CREATE);
        List<CloudServiceKey> keys = serviceKeys.get(service.getResourceName());
        return CollectionUtils.isNotEmpty(keys);
    }

    private boolean shouldDeleteServiceKeys(CloudServiceInstance existingService, ProcessContext context) {
        if (existingService == null) {
            return false;
        }
        return StepsUtil.canDeleteServiceKeys(context);
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
        existingServiceTags = ObjectUtils.defaultIfNull(existingServiceTags, Collections.emptyList());
        return !existingServiceTags.equals(service.getTags());
    }

    private boolean shouldUpdateCredentials(CloudServiceInstanceExtended service, CloudServiceInstance existingService,
                                            CloudControllerClient client) {
        try {
            Map<String, Object> serviceParameters = getServiceInstanceParameters(client, existingService);
            getStepLogger().debug("Existing service parameters: " + SecureSerialization.toJson(serviceParameters));
            return !Objects.equals(service.getCredentials(), serviceParameters);
        } catch (CloudOperationException e) {
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST || service.isOptional()) {
                getStepLogger().warnWithoutProgressMessage(Messages.CANNOT_RETRIEVE_SERVICE_INSTANCE_PARAMETERS, service.getName());
                // TODO: Optimization (Hack) that should be deprecated at some point. So here is a todo for that.
                return !MapUtils.isEmpty(service.getCredentials());
            }
            throw e;
        }
    }

    private Map<String, Object> getServiceInstanceParameters(CloudControllerClient client, CloudServiceInstance existingService) {
        if (existingService.isUserProvided()) {
            return client.getUserProvidedServiceInstanceParameters(existingService.getMetadata()
                                                                                  .getGuid());
        }
        return client.getServiceInstanceParameters(existingService.getMetadata()
                                                                  .getGuid());
    }

    private boolean shouldUpdateSyslogUrl(CloudServiceInstance service, String existingSyslogUrl) {
        if (!service.isUserProvided()) {
            return false;
        }
        String syslogDrainUrl = ObjectUtils.defaultIfNull(service.getSyslogDrainUrl(), "");
        return !Objects.equals(syslogDrainUrl, existingSyslogUrl);
    }
}
