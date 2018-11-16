package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.CloudServiceBrokerException;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.XsCloudControllerClient;
import com.sap.cloud.lm.sl.cf.core.cf.clients.EventsGetter;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceGetter;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.CommonUtil;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Component("deleteServicesStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteServicesStep extends AsyncFlowableStep {

    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Inject
    private ServiceGetter serviceGetter;

    @Inject
    private EventsGetter eventsGetter;

    @Override
    protected StepPhase executeAsyncStep(ExecutionWrapper execution) throws Exception {
        try {
            getStepLogger().debug(Messages.DELETING_SERVICES);

            CloudControllerClient client = execution.getControllerClient();

            List<String> servicesToDelete = StepsUtil.getServicesToDelete(execution.getContext());

            XsCloudControllerClient xsClient = execution.getXsControllerClient();
            if (xsClient == null) {
                Map<String, String> serviceGuids = getServicesGuids(servicesToDelete, execution);
                validateGuidMapping(servicesToDelete, serviceGuids);
                StepsUtil.setServicesGuids(execution.getContext(), serviceGuids);
            }

            Map<String, ServiceOperationType> triggeredServiceOperations = deleteServices(client, servicesToDelete);

            execution.getStepLogger()
                .debug(Messages.TRIGGERED_SERVICE_OPERATIONS, JsonUtil.toJson(triggeredServiceOperations, true));
            StepsUtil.setTriggeredServiceOperations(execution.getContext(), triggeredServiceOperations);

            getStepLogger().debug(Messages.SERVICES_DELETED);
            return StepPhase.POLL;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_DELETING_SERVICES);
            throw e;
        }
    }

    private Map<String, String> getServicesGuids(List<String> services, ExecutionWrapper execution) {
        String spaceId = StepsUtil.getSpaceId(execution.getContext());
        CloudControllerClient client = execution.getControllerClient();

        return services.stream()
            .map(name -> serviceGetter.getServiceInstance(client, name, spaceId))
            .filter(instance -> instance != null && !instance.isEmpty())
            .collect(Collectors.toMap(this::getNameFromInstance, this::getGuidFromInstance));
    }

    private void validateGuidMapping(List<String> servicesToDelete, Map<String, String> serviceGuids) {
        List<String> servicesWithoutGuid = servicesToDelete.stream()
            .filter(name -> serviceGuids.get(name) == null)
            .collect(Collectors.toList());

        if (!servicesWithoutGuid.isEmpty()) {
            throw new SLException(Messages.ERROR_MAPPING_SERVICE_NAMES_TO_GUIDS, String.join(",", servicesWithoutGuid));
        }
    }

    private String getGuidFromInstance(Map<String, Object> serviceInstance) {
        Map<String, Object> metadata = CommonUtil.cast(serviceInstance.get("metadata"));
        return CommonUtil.cast(metadata.get("guid"));
    }

    private String getNameFromInstance(Map<String, Object> serviceInstance) {
        Map<String, Object> metadata = CommonUtil.cast(serviceInstance.get("entity"));
        return CommonUtil.cast(metadata.get("name"));
    }

    private Map<String, ServiceOperationType> deleteServices(CloudControllerClient client, List<String> serviceNames) {
        Map<String, ServiceOperationType> triggeredServiceOperations = new HashMap<>();

        List<CloudServiceInstance> serviceInstances = getServiceInstances(serviceNames, client);
        List<CloudServiceInstance> deletableServices = getDeletableServices(serviceInstances);

        for (CloudServiceInstance instance : deletableServices) {
            try {
                deleteService(client, instance.getService()
                    .getName());
                triggeredServiceOperations.put(instance.getService()
                    .getName(), ServiceOperationType.DELETE);
            } catch (CloudOperationException | CloudException e) {
                processException(e, instance, instance.getService()
                    .getName());
            }
        }
        return triggeredServiceOperations;
    }

    private List<CloudServiceInstance> getServiceInstances(List<String> serviceNames, CloudControllerClient client) {
        return serviceNames.parallelStream()
            .map(client::getServiceInstance)
            .collect(Collectors.toList());
    }

    private List<CloudServiceInstance> getDeletableServices(List<CloudServiceInstance> serviceInstances) {
        return serviceInstances.parallelStream()
            .filter(serviceInstance -> validateServiceHasNoBindings(serviceInstance, serviceInstance.getName()))
            .collect(Collectors.toList());
    }

    private void deleteService(CloudControllerClient client, String serviceName) {
        getStepLogger().info(Messages.DELETING_SERVICE, serviceName);
        client.deleteService(serviceName);
        getStepLogger().debug(Messages.SERVICE_DELETED, serviceName);
    }

    private boolean validateServiceHasNoBindings(CloudServiceInstance serviceInstance, String serviceName) {
        List<CloudServiceBinding> bindings = serviceInstance.getBindings();
        if (!CollectionUtils.isEmpty(bindings)) {
            logBindings(bindings);
            getStepLogger().info(Messages.SERVICE_HAS_BINDINGS_AND_CANNOT_BE_DELETED, serviceName);
            return false;
        }
        return true;
    }

    private void processException(Exception e, CloudServiceInstance serviceInstance, String serviceName) {
        if (e instanceof CloudOperationException) {
            e = evaluateCloudOperationException((CloudOperationException) e, serviceName);
            if (e == null) {
                return;
            }
        }
        wrapAndThrowException(e, serviceInstance, serviceName);
    }

    private CloudOperationException evaluateCloudOperationException(CloudOperationException e, String serviceName) {
        if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
            getStepLogger().warn(e, Messages.COULD_NOT_DELETE_SERVICE, serviceName);
            return null;
        }
        if (e.getStatusCode() == HttpStatus.BAD_GATEWAY) {
            return new CloudServiceBrokerException(e);
        }
        return new CloudControllerException(e);

    }

    private void wrapAndThrowException(Exception e, CloudServiceInstance serviceInstance, String serviceName) {
        String msg = buildNewExceptionMessage(e, serviceInstance, serviceName);
        throw new SLException(e, msg);
    }

    private String buildNewExceptionMessage(Exception e, CloudServiceInstance serviceInstance, String serviceName) {
        if (serviceInstance == null) {
            return MessageFormat.format(Messages.ERROR_DELETING_SERVICE_SHORT, serviceName, e.getMessage());
        }
        CloudService service = serviceInstance.getService();
        return MessageFormat.format(Messages.ERROR_DELETING_SERVICE, service.getName(), service.getLabel(), service.getPlan(),
            e.getMessage());
    }

    private void logBindings(List<CloudServiceBinding> bindings) {
        getStepLogger().debug(Messages.SERVICE_BINDINGS_EXISTS, secureSerializer.toJson(bindings));
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ExecutionWrapper execution) {
        return Arrays.asList(new PollServiceDeleteOperationsExecution(eventsGetter)); 
    }

}
