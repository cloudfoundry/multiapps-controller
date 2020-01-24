package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudServiceKey;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.model.ServiceOperation;
import com.sap.cloud.lm.sl.cf.core.util.MethodExecution;
import com.sap.cloud.lm.sl.cf.core.util.MethodExecution.ExecutionState;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ServiceOperationExecutor;

@Named("updateServiceKeysStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateServiceKeysStep extends ServiceStep {

    @Inject
    private ServiceOperationExecutor serviceOperationExecutor;

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ExecutionWrapper execution) {
        throw new UnsupportedOperationException("Update service keys is not a pollable operation.");
    }

    private MethodExecution<String> createOrUpdateServiceKeys(CloudServiceExtended service, DelegateExecution execution,
                                                              CloudControllerClient client) {
        MethodExecution<String> methodExecution = new MethodExecution<>(null, ExecutionState.FINISHED);
        Map<String, List<CloudServiceKey>> serviceKeysMap = StepsUtil.getServiceKeysToCreate(execution);
        List<CloudServiceKey> serviceKeys = serviceKeysMap.get(service.getResourceName());

        List<CloudServiceKey> existingServiceKeys = serviceOperationExecutor.executeServiceOperation(service,
                                                                                                     () -> client.getServiceKeys(service.getName()),
                                                                                                     getStepLogger());

        if (existingServiceKeys == null) {
            return methodExecution;
        }

        List<CloudServiceKey> serviceKeysToCreate = getServiceKeysToCreate(serviceKeys, existingServiceKeys);
        List<CloudServiceKey> serviceKeysToUpdate = getServiceKeysToUpdate(serviceKeys, existingServiceKeys);
        List<CloudServiceKey> serviceKeysToDelete = getServiceKeysToDelete(serviceKeys, existingServiceKeys);

        if (canDeleteServiceKeys(execution)) {
            deleteServiceKeys(client, serviceKeysToDelete);
            // Recreate the service keys, which should be updated, as direct update is not supported
            // by the controller:
            deleteServiceKeys(client, serviceKeysToUpdate);
            createServiceKeys(client, serviceKeysToUpdate);
        } else {
            serviceKeysToDelete.forEach(key -> getStepLogger().warn(Messages.WILL_NOT_DELETE_SERVICE_KEY, key.getName(), key.getService()
                                                                                                                            .getName()));
            serviceKeysToUpdate.forEach(key -> getStepLogger().warn(Messages.WILL_NOT_UPDATE_SERVICE_KEY, key.getName(), key.getService()
                                                                                                                            .getName()));
        }
        createServiceKeys(client, serviceKeysToCreate);
        return methodExecution;
    }

    private List<CloudServiceKey> getServiceKeysToCreate(List<CloudServiceKey> serviceKeys, List<CloudServiceKey> existingServiceKeys) {
        return serviceKeys.stream()
                          .filter(key -> shouldCreate(key, existingServiceKeys))
                          .collect(Collectors.toList());
    }

    private List<CloudServiceKey> getServiceKeysToUpdate(List<CloudServiceKey> serviceKeys, List<CloudServiceKey> existingServiceKeys) {
        return serviceKeys.stream()
                          .filter(key -> shouldUpdate(key, existingServiceKeys))
                          .collect(Collectors.toList());
    }

    private List<CloudServiceKey> getServiceKeysToDelete(List<CloudServiceKey> serviceKeys, List<CloudServiceKey> existingServiceKeys) {
        return existingServiceKeys.stream()
                                  .filter(existingkey -> shouldDelete(existingkey, serviceKeys))
                                  .collect(Collectors.toList());
    }

    private boolean shouldCreate(CloudServiceKey key, List<CloudServiceKey> existingKeys) {
        return getWithName(existingKeys, key.getName()) == null;
    }

    private boolean shouldUpdate(CloudServiceKey key, List<CloudServiceKey> existingKeys) {
        CloudServiceKey existingKey = getWithName(existingKeys, key.getName());
        return (existingKey != null) && (!areServiceKeysEqual(key, existingKey));
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

    private boolean canDeleteServiceKeys(DelegateExecution context) {
        return (boolean) context.getVariable(Constants.PARAM_DELETE_SERVICE_KEYS);
    }

    private boolean areServiceKeysEqual(CloudServiceKey key1, CloudServiceKey key2) {
        return Objects.equals(key1.getCredentials(), key2.getCredentials()) && Objects.equals(key1.getName(), key2.getName());
    }

    private void deleteServiceKeys(CloudControllerClient client, List<CloudServiceKey> serviceKeys) {
        serviceKeys.forEach(key -> deleteServiceKey(client, key));
    }

    private void createServiceKeys(CloudControllerClient client, List<CloudServiceKey> serviceKeys) {
        serviceKeys.forEach(key -> createServiceKey(client, key));
    }

    private void createServiceKey(CloudControllerClient client, CloudServiceKey key) {
        getStepLogger().info(Messages.CREATING_SERVICE_KEY_FOR_SERVICE, key.getName(), key.getService()
                                                                                          .getName());
        client.createServiceKey(key.getService()
                                   .getName(),
                                key.getName(), key.getCredentials());
        getStepLogger().debug(Messages.CREATED_SERVICE_KEY, key.getName());
    }

    private void deleteServiceKey(CloudControllerClient client, CloudServiceKey key) {
        getStepLogger().info(Messages.DELETING_SERVICE_KEY_FOR_SERVICE, key.getName(), key.getService()
                                                                                          .getName());
        client.deleteServiceKey(key.getService()
                                   .getName(),
                                key.getName());
    }

    @Override
    protected ServiceOperation.Type getOperationType() {
        return ServiceOperation.Type.UPDATE;
    }

    @Override
    protected MethodExecution<String> executeOperation(DelegateExecution context, CloudControllerClient controllerClient,
                                                       CloudServiceExtended service) {
        return createOrUpdateServiceKeys(service, context, controllerClient);
    }
}
