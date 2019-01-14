package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.CloudServiceBrokerException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.ServiceKey;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.XsCloudControllerClient;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.EventsGetter;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Component("deleteServicesStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteServicesStep extends AsyncFlowableStep {

    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Inject
    private EventsGetter eventsGetter;

    @Override
    protected StepPhase executeAsyncStep(ExecutionWrapper execution) throws Exception {
        try {
            getStepLogger().debug(Messages.DELETING_SERVICES);

            CloudControllerClient client = execution.getControllerClient();

            List<String> servicesToDelete = new ArrayList<>(StepsUtil.getServicesToDelete(execution.getContext()));

            if (servicesToDelete.isEmpty()) {
                getStepLogger().debug(Messages.MISSING_SERVICES_TO_DELETE);
                return StepPhase.DONE;
            }

            XsCloudControllerClient xsClient = execution.getXsControllerClient();
            if (xsClient == null) {
                Map<String, CloudServiceExtended> servicesData = getServicesData(servicesToDelete, execution);
                List<String> servicesWithoutData = getServicesWithoutData(servicesToDelete, servicesData);
                if(!servicesWithoutData.isEmpty()) {
                    execution.getStepLogger()
                    .info(Messages.SERVICES_ARE_ALREADY_DELETED, servicesWithoutData);
                    servicesToDelete.removeAll(servicesWithoutData);
                }
                StepsUtil.setServicesData(execution.getContext(), servicesData);
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

    private Map<String, CloudServiceExtended> getServicesData(List<String> serviceNames, ExecutionWrapper execution) {
        CloudControllerClient client = execution.getControllerClient();

        return serviceNames.parallelStream()
            .map(name -> client.getService(name, false))
            .filter(Objects::nonNull)
            .map(service -> new CloudServiceExtended(service.getMeta(), service.getName()))
            .collect(Collectors.toMap(e -> e.getName(), e -> e));
    }

    private List<String> getServicesWithoutData(List<String> servicesToDelete, Map<String, CloudServiceExtended> servicesData) {
        return servicesToDelete.stream()
            .filter(name -> servicesData.get(name) == null)
            .collect(Collectors.toList());
    }

    private Map<String, ServiceOperationType> deleteServices(CloudControllerClient client, List<String> serviceNames) {
        Map<String, ServiceOperationType> triggeredServiceOperations = new HashMap<>();

        for (String serviceName : serviceNames) {
            try {
                prepareServicesToDelete(client, serviceName);
                deleteService(client, serviceName);
                triggeredServiceOperations.put(serviceName, ServiceOperationType.DELETE);
            } catch (CloudOperationException | CloudException e) {
                processException(e, client.getServiceInstance(serviceName), serviceName);
            }
        }
        return triggeredServiceOperations;
    }

    private void prepareServicesToDelete(CloudControllerClient client, String serviceName) {
        unbindService(client, serviceName);
        deleteServiceKeys(client, serviceName);
    }

    private void unbindService(CloudControllerClient client, String serviceName) {
        CloudServiceInstance serviceInstance = client.getServiceInstance(serviceName);
        List<CloudServiceBinding> bindings = serviceInstance.getBindings();
        if (bindings.isEmpty()) {
            return;
        }
        logBindings(bindings);
        for (CloudServiceBinding binding : bindings) {
            CloudApplication application = StepsUtil.getBoundApplication(client.getApplications(), binding.getAppGuid());
            getStepLogger().info(Messages.UNBINDING_APP_FROM_SERVICE, application.getName(), serviceName);
            client.unbindService(application.getName(), serviceName);
        }
    }

    private void deleteServiceKeys(CloudControllerClient client, String serviceName) {
        CloudService service = client.getService(serviceName);
        if (service.isUserProvided()) {
            return;
        }
        List<ServiceKey> serviceKeys = client.getServiceKeys(serviceName);
        for (ServiceKey serviceKey : serviceKeys) {
            getStepLogger().info(Messages.DELETING_SERVICE_KEY_FOR_SERVICE, serviceKey.getName(), serviceName);
            client.deleteServiceKey(serviceName, serviceKey.getName());
        }
    }

    private void deleteService(CloudControllerClient client, String serviceName) {
        getStepLogger().info(Messages.DELETING_SERVICE, serviceName);
        client.deleteService(serviceName);
        getStepLogger().debug(Messages.SERVICE_DELETED, serviceName);
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
