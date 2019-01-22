package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CyclicBarrier;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.ServiceKey;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.core.exec.MethodExecution;
import com.sap.cloud.lm.sl.cf.core.exec.MethodExecution.ExecutionState;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ServiceOperationExecutor;

@Component("updateServiceKeysStep")
public class UpdateServiceKeysStep extends ServiceStep {

    private ServiceOperationExecutor serviceOperationExecutor = new ServiceOperationExecutor();
    
    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ExecutionWrapper execution) {
        throw new UnsupportedOperationException("Update service keys is not a pollable operation.");
    }

    private MethodExecution<String> createOrUpdateServiceKeys(CloudServiceExtended service, DelegateExecution execution,
        CloudControllerClient client) {
        MethodExecution<String> methodExecution = new MethodExecution<String>(null, ExecutionState.FINISHED);
        Map<String, List<ServiceKey>> serviceKeysMap = StepsUtil.getServiceKeysToCreate(execution);
        List<ServiceKey> serviceKeys = serviceKeysMap.get(service.getName());

        List<ServiceKey> existingServiceKeys = serviceOperationExecutor.executeServiceOperation(service,
            (Supplier<List<ServiceKey>>) () -> client.getServiceKeys(service.getName()), getStepLogger());

        if (existingServiceKeys == null) {
            return methodExecution;
        }

        List<ServiceKey> serviceKeysToCreate = getServiceKeysToCreate(serviceKeys, existingServiceKeys);
        List<ServiceKey> serviceKeysToUpdate = getServiceKeysToUpdate(serviceKeys, existingServiceKeys);
        List<ServiceKey> serviceKeysToDelete = getServiceKeysToDelete(serviceKeys, existingServiceKeys);

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

    private List<ServiceKey> getServiceKeysToCreate(List<ServiceKey> serviceKeys, List<ServiceKey> existingServiceKeys) {
        return serviceKeys.stream()
            .filter(key -> shouldCreate(key, existingServiceKeys))
            .collect(Collectors.toList());
    }

    private List<ServiceKey> getServiceKeysToUpdate(List<ServiceKey> serviceKeys, List<ServiceKey> existingServiceKeys) {
        return serviceKeys.stream()
            .filter(key -> shouldUpdate(key, existingServiceKeys))
            .collect(Collectors.toList());
    }

    private List<ServiceKey> getServiceKeysToDelete(List<ServiceKey> serviceKeys, List<ServiceKey> existingServiceKeys) {
        return existingServiceKeys.stream()
            .filter(key -> shouldDelete(key, serviceKeys))
            .collect(Collectors.toList());
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
        return serviceKeys.stream()
            .filter(key -> key.getName()
                .equals(name))
            .findAny()
            .orElse(null);
    }

    private boolean canDeleteServiceKeys(DelegateExecution context) {
        return (Boolean) context.getVariable(Constants.PARAM_DELETE_SERVICE_KEYS);
    }

    private boolean areServiceKeysEqual(ServiceKey key1, ServiceKey key2) {
        return Objects.equals(key1.getParameters(), key2.getParameters()) && Objects.equals(key1.getName(), key2.getName());
    }

    private void deleteServiceKeys(CloudControllerClient client, List<ServiceKey> serviceKeys) {
        serviceKeys.stream()
            .forEach(key -> deleteServiceKey(client, key));
    }

    private void createServiceKeys(CloudControllerClient client, List<ServiceKey> serviceKeys) {
        serviceKeys.stream()
            .forEach(key -> createServiceKey(client, key));
    }

    private void createServiceKey(CloudControllerClient client, ServiceKey key) {
        getStepLogger().info(Messages.CREATING_SERVICE_KEY_FOR_SERVICE, key.getName(), key.getService()
            .getName());
        client.createServiceKey(key.getService()
            .getName(), key.getName(), key.getParameters());
        getStepLogger().debug(Messages.CREATED_SERVICE_KEY, key.getName());
    }

    private void deleteServiceKey(CloudControllerClient client, ServiceKey key) {
        getStepLogger().info(Messages.DELETING_SERVICE_KEY_FOR_SERVICE, key.getName(), key.getService()
            .getName());
        client.deleteServiceKey(key.getService()
            .getName(), key.getName());
    }

    @Override
    protected ServiceOperationType getOperationType() {
        return ServiceOperationType.UPDATE;
    }

    @Override
    protected MethodExecution<String> executeOperation(DelegateExecution context, CloudControllerClient controllerClient,
        CloudServiceExtended service) {
        return createOrUpdateServiceKeys(service, context, controllerClient);
    }
}
