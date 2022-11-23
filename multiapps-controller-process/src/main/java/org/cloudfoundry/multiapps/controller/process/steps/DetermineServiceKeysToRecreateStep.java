package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataAnnotations;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ServiceUtil;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ServiceCredentialBindingOperation;

@Named("determineServiceKeysToRecreateStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DetermineServiceKeysToRecreateStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) throws Exception {
        CloudServiceInstanceExtended serviceToProcess = context.getVariable(Variables.SERVICE_TO_PROCESS);
        Map<String, List<CloudServiceKey>> serviceKeysMap = context.getVariable(Variables.SERVICE_KEYS_TO_CREATE);
        List<CloudServiceKey> serviceKeys = serviceKeysMap.get(serviceToProcess.getResourceName());
        CloudControllerClient controllerClient = context.getControllerClient();
        List<CloudServiceKey> existingServiceKeys = ServiceUtil.getExistingServiceKeys(controllerClient, serviceToProcess, getStepLogger());
        if (existingServiceKeys == null) {
            context.setVariable(Variables.CLOUD_SERVICE_KEYS_TO_DELETE, Collections.emptyList());
            context.setVariable(Variables.CLOUD_SERVICE_KEYS_TO_CREATE, Collections.emptyList());
            return StepPhase.DONE;
        }
        return calculateServiceKeysForRecreation(context, serviceKeys, existingServiceKeys);
    }

    private StepPhase calculateServiceKeysForRecreation(ProcessContext context, List<CloudServiceKey> serviceKeys,
                                                        List<CloudServiceKey> existingServiceKeys) {
        String namespace = context.getVariable(Variables.MTA_NAMESPACE);
        List<CloudServiceKey> existingKeysForNamespace = getFilteredServiceKeys(existingServiceKeys,
                                                                                key -> hasSameNamespace(key, namespace));
        List<CloudServiceKey> serviceKeysToCreate = getFilteredServiceKeys(serviceKeys, key -> shouldCreate(key, existingServiceKeys));
        List<CloudServiceKey> serviceKeysToUpdate = getFilteredServiceKeys(serviceKeys, key -> shouldUpdate(key, existingServiceKeys));
        List<CloudServiceKey> serviceKeysToDelete = getFilteredServiceKeys(existingKeysForNamespace, key -> shouldDelete(key, serviceKeys));
        return determineServiceKeysForRecreation(context, serviceKeysToCreate, serviceKeysToUpdate, serviceKeysToDelete);
    }

    private StepPhase determineServiceKeysForRecreation(ProcessContext context, List<CloudServiceKey> serviceKeysToCreate,
                                                        List<CloudServiceKey> serviceKeysToUpdate,
                                                        List<CloudServiceKey> serviceKeysToDelete) {
        List<CloudServiceKey> allServiceKeysToCreate = new ArrayList<>();
        if (canDeleteServiceKeys(context)) {
            List<CloudServiceKey> allServiceKeysToDelete = ListUtils.union(serviceKeysToDelete, serviceKeysToUpdate);
            context.setVariable(Variables.CLOUD_SERVICE_KEYS_TO_DELETE, allServiceKeysToDelete);
            getStepLogger().debug(Messages.SERVICE_KEYS_SCHEDULED_FOR_DELETION_0, JsonUtil.toJson(allServiceKeysToDelete));
            allServiceKeysToCreate.addAll(serviceKeysToUpdate);
        } else {
            serviceKeysToDelete.forEach(this::logWillNotDeleteMessage);
            serviceKeysToUpdate.forEach(this::logWillNotUpdateMessage);
            context.setVariable(Variables.CLOUD_SERVICE_KEYS_TO_DELETE, Collections.emptyList());
        }
        allServiceKeysToCreate.addAll(serviceKeysToCreate);
        context.setVariable(Variables.CLOUD_SERVICE_KEYS_TO_CREATE, allServiceKeysToCreate);
        getStepLogger().debug(Messages.SERVICE_KEYS_SCHEDULED_FOR_CREATION_0, JsonUtil.toJson(allServiceKeysToCreate));
        return StepPhase.DONE;
    }

    private List<CloudServiceKey> getFilteredServiceKeys(List<CloudServiceKey> serviceKeys, Predicate<CloudServiceKey> filter) {
        return serviceKeys.stream()
                          .filter(filter)
                          .collect(Collectors.toList());
    }

    private boolean shouldCreate(CloudServiceKey key, List<CloudServiceKey> existingKeys) {
        return getWithName(existingKeys, key.getName()) == null;
    }

    private boolean shouldUpdate(CloudServiceKey key, List<CloudServiceKey> existingKeys) {
        CloudServiceKey existingKey = getWithName(existingKeys, key.getName());
        if (existingKey == null) {
            return false;
        }
        return !areServiceKeysEqual(key, existingKey) || isServiceKeyStateNotSucceeded(existingKey);
    }

    private boolean isServiceKeyStateNotSucceeded(CloudServiceKey existingKey) {
        return existingKey.getServiceKeyOperation()
                          .getState() != ServiceCredentialBindingOperation.State.SUCCEEDED;
    }

    private boolean areServiceKeysEqual(CloudServiceKey key1, CloudServiceKey key2) {
        return Objects.equals(key1.getCredentials(), key2.getCredentials()) && Objects.equals(key1.getName(), key2.getName());
    }

    private boolean shouldDelete(CloudServiceKey existingKey, List<CloudServiceKey> keys) {
        return getWithName(keys, existingKey.getName()) == null;
    }

    private CloudServiceKey getWithName(List<CloudServiceKey> serviceKeys, String name) {
        return serviceKeys.stream()
                          .filter(key -> key.getName()
                                            .equals(name))
                          .findAny()
                          .orElse(null);
    }

    private boolean hasSameNamespace(CloudServiceKey serviceKey, String namespace) {
        String keyNamespace = null;
        if (serviceKey.getV3Metadata() != null && serviceKey.getV3Metadata()
                                                            .getAnnotations() != null) {
            keyNamespace = serviceKey.getV3Metadata()
                                     .getAnnotations()
                                     .get(MtaMetadataAnnotations.MTA_NAMESPACE);
        }

        if (StringUtils.isEmpty(keyNamespace)) {
            return StringUtils.isEmpty(namespace);
        } else {
            return keyNamespace.equals(namespace);
        }
    }

    private boolean canDeleteServiceKeys(ProcessContext context) {
        return context.getVariable(Variables.DELETE_SERVICE_KEYS);
    }

    private void logWillNotDeleteMessage(CloudServiceKey key) {
        getStepLogger().warn(Messages.WILL_NOT_DELETE_SERVICE_KEY, key.getName(), key.getServiceInstance()
                                                                                     .getName());
    }

    private void logWillNotUpdateMessage(CloudServiceKey key) {
        getStepLogger().warn(Messages.WILL_NOT_UPDATE_SERVICE_KEY, key.getName(), key.getServiceInstance()
                                                                                     .getName());
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_WHILE_DETERMINING_SERVICE_KEYS_TO_RECREATE;
    }

}
