package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataAnnotations;
import org.cloudfoundry.multiapps.controller.core.util.OperationExecutionState;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.eclipse.jgit.util.StringUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudControllerException;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.CloudServiceBrokerException;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ServiceOperation;

@Named("updateServiceKeysStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateServiceKeysStep extends ServiceStep {

    // TODO: This step does not exist in the diagrams and it is only for 1 tact backwards compatibility. Delete after 1 tact!
    @Override
    protected OperationExecutionState executeOperation(ProcessContext context, CloudControllerClient client,
                                                       CloudServiceInstanceExtended service) {
        Map<String, List<CloudServiceKey>> serviceKeysMap = context.getVariable(Variables.SERVICE_KEYS_TO_CREATE);
        List<CloudServiceKey> serviceKeys = serviceKeysMap.get(service.getResourceName());
        List<CloudServiceKey> existingServiceKeys = getExistingServiceKeys(client, service);
        String namespace = context.getVariable(Variables.MTA_NAMESPACE);

        if (existingServiceKeys == null) {
            return OperationExecutionState.FINISHED;
        }
        List<CloudServiceKey> existingKeysForNamespace = getFilteredServiceKeys(existingServiceKeys,
                                                                                key -> hasSameNamespace(key, namespace));

        List<CloudServiceKey> serviceKeysToCreate = getFilteredServiceKeys(serviceKeys, key -> shouldCreate(key, existingServiceKeys));
        List<CloudServiceKey> serviceKeysToUpdate = getFilteredServiceKeys(serviceKeys, key -> shouldUpdate(key, existingServiceKeys));
        List<CloudServiceKey> serviceKeysToDelete = getFilteredServiceKeys(existingKeysForNamespace, key -> shouldDelete(key, serviceKeys));

        if (canDeleteServiceKeys(context)) {
            deleteServiceKeys(serviceKeysToDelete, client);
            // Recreate the service keys, which should be updated, as direct update is not supported
            // by the controller:
            deleteServiceKeys(serviceKeysToUpdate, client);
            createServiceKeys(serviceKeysToUpdate, client);
        } else {
            serviceKeysToDelete.forEach(this::logWillNotDeleteMessage);
            serviceKeysToUpdate.forEach(this::logWillNotUpdateMessage);
        }

        createServiceKeys(serviceKeysToCreate, client);

        return OperationExecutionState.FINISHED;
    }

    private boolean hasSameNamespace(CloudServiceKey serviceKey, String namespace) {
        String keyNamespace = null;
        if (serviceKey.getV3Metadata() != null && serviceKey.getV3Metadata()
                                                            .getAnnotations() != null) {
            keyNamespace = serviceKey.getV3Metadata()
                                     .getAnnotations()
                                     .get(MtaMetadataAnnotations.MTA_NAMESPACE);
        }

        if (StringUtils.isEmptyOrNull(keyNamespace)) {
            return StringUtils.isEmptyOrNull(namespace);
        } else {
            return keyNamespace.equals(namespace);
        }
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ProcessContext context) {
        throw new UnsupportedOperationException("Update service keys is not a pollable operation.");
    }

    @Override
    protected ServiceOperation.Type getOperationType() {
        return ServiceOperation.Type.UPDATE;
    }

    private List<CloudServiceKey> getExistingServiceKeys(CloudControllerClient client, CloudServiceInstanceExtended service) {
        try {
            return client.getServiceKeysWithCredentials(service.getName());
        } catch (CloudOperationException e) {
            if (!service.isOptional()) {
                if (e.getStatusCode() == HttpStatus.BAD_GATEWAY) {
                    throw new CloudServiceBrokerException(e);
                }
                throw new CloudControllerException(e);
            }
            getStepLogger().warn(e, Messages.COULD_NOT_GET_SERVICE_KEYS_FOR_OPTIONAL_SERVICE, service.getName());
            return null;
        }
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
        return existingKey != null && !areServiceKeysEqual(key, existingKey);
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

    private boolean canDeleteServiceKeys(ProcessContext context) {
        return context.getVariable(Variables.DELETE_SERVICE_KEYS);
    }

    private boolean areServiceKeysEqual(CloudServiceKey key1, CloudServiceKey key2) {
        return Objects.equals(key1.getCredentials(), key2.getCredentials()) && Objects.equals(key1.getName(), key2.getName());
    }

    private void createServiceKeys(List<CloudServiceKey> serviceKeys, CloudControllerClient client) {
        for (CloudServiceKey key : serviceKeys) {
            createServiceKey(client, key);
        }
    }

    private void deleteServiceKeys(List<CloudServiceKey> serviceKeys, CloudControllerClient client) {
        for (CloudServiceKey key : serviceKeys) {
            deleteServiceKey(client, key);
        }
    }

    private void logWillNotDeleteMessage(CloudServiceKey key) {
        getStepLogger().warn(Messages.WILL_NOT_DELETE_SERVICE_KEY, key.getName(), key.getServiceInstance()
                                                                                     .getName());
    }

    private void logWillNotUpdateMessage(CloudServiceKey key) {
        getStepLogger().warn(Messages.WILL_NOT_UPDATE_SERVICE_KEY, key.getName(), key.getServiceInstance()
                                                                                     .getName());
    }

    private void createServiceKey(CloudControllerClient client, CloudServiceKey key) {
        getStepLogger().info(Messages.CREATING_SERVICE_KEY_FOR_SERVICE_INSTANCE, key.getName(), key.getServiceInstance()
                                                                                                   .getName());
        client.createAndFetchServiceKey(key, key.getServiceInstance()
                                                .getName());
        getStepLogger().debug(Messages.CREATED_SERVICE_KEY, key.getName());
    }

    private void deleteServiceKey(CloudControllerClient client, CloudServiceKey key) {
        getStepLogger().info(Messages.DELETING_SERVICE_KEY_FOR_SERVICE_INSTANCE, key.getName(), key.getServiceInstance()
                                                                                                   .getName());
        if (key.getMetadata() != null) {
            client.deleteServiceBindingSync(key.getGuid());
            return;
        }
        CloudServiceKey serviceKey = client.getServiceKey(key.getServiceInstance()
                                                             .getName(),
                                                          key.getName());
        client.deleteServiceBindingSync(serviceKey.getGuid());
    }
}
