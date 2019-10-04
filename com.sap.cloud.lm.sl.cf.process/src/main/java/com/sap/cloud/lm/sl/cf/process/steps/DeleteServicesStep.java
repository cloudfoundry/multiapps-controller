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
import javax.inject.Named;

import org.apache.commons.collections4.ListUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.CloudServiceBrokerException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.CloudServiceKey;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.helpers.ExceptionMessageTailMapper;
import com.sap.cloud.lm.sl.cf.process.helpers.ExceptionMessageTailMapper.CloudComponents;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ServiceOperationGetter;
import com.sap.cloud.lm.sl.cf.process.util.ServiceProgressReporter;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Named("deleteServicesStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteServicesStep extends AsyncFlowableStep {

    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    private ServiceOperationGetter serviceOperationGetter;
    private ServiceProgressReporter serviceProgressReporter;

    @Inject
    public DeleteServicesStep(ServiceOperationGetter serviceOperationGetter, ServiceProgressReporter serviceProgressReporter) {
        this.serviceOperationGetter = serviceOperationGetter;
        this.serviceProgressReporter = serviceProgressReporter;
    }

    @Override
    protected StepPhase executeAsyncStep(ExecutionWrapper execution) {
        getStepLogger().debug(Messages.DELETING_SERVICES);

        CloudControllerClient client = execution.getControllerClient();

        List<String> servicesToDelete = new ArrayList<>(StepsUtil.getServicesToDelete(execution.getContext()));

        if (servicesToDelete.isEmpty()) {
            getStepLogger().debug(Messages.MISSING_SERVICES_TO_DELETE);
            return StepPhase.DONE;
        }

        List<CloudServiceExtended> servicesData = getServicesData(servicesToDelete, execution);
        List<String> servicesWithoutData = getServicesWithoutData(servicesToDelete, servicesData);
        if (!servicesWithoutData.isEmpty()) {
            execution.getStepLogger()
                     .info(Messages.SERVICES_ARE_ALREADY_DELETED, servicesWithoutData);
            servicesToDelete.removeAll(servicesWithoutData);
        }
        StepsUtil.setServicesData(execution.getContext(), servicesData);

        Map<String, ServiceOperationType> triggeredServiceOperations = deleteServices(execution.getContext(), client, servicesToDelete);

        execution.getStepLogger()
                 .debug(Messages.TRIGGERED_SERVICE_OPERATIONS, JsonUtil.toJson(triggeredServiceOperations, true));
        StepsUtil.setTriggeredServiceOperations(execution.getContext(), triggeredServiceOperations);

        getStepLogger().debug(Messages.SERVICES_DELETED);
        return StepPhase.POLL;
    }

    @Override
    protected String getStepErrorMessage(DelegateExecution context) {
        return Messages.ERROR_DELETING_SERVICES;
    }

    @Override
    protected String getStepErrorMessageAdditionalDescription(DelegateExecution context) {
        String offering = StepsUtil.getServiceOffering(context);
        return ExceptionMessageTailMapper.map(configuration, CloudComponents.SERVICE_BROKERS, offering);
    }

    private List<CloudServiceExtended> getServicesData(List<String> serviceNames, ExecutionWrapper execution) {
        CloudControllerClient client = execution.getControllerClient();

        return serviceNames.parallelStream()
                           .map(name -> client.getService(name, false))
                           .filter(Objects::nonNull)
                           .map(this::buildCloudServiceExtended)
                           .collect(Collectors.toList());
    }

    private ImmutableCloudServiceExtended buildCloudServiceExtended(CloudService service) {
        return ImmutableCloudServiceExtended.builder()
                                            .metadata(service.getMetadata())
                                            .name(service.getName())
                                            .build();
    }

    private List<String> getServicesWithoutData(List<String> servicesToDelete, List<CloudServiceExtended> servicesData) {
        List<String> servicesWithDataNames = servicesData.stream()
                                                         .map(CloudServiceExtended::getName)
                                                         .collect(Collectors.toList());
        return ListUtils.removeAll(servicesToDelete, servicesWithDataNames);
    }

    private Map<String, ServiceOperationType> deleteServices(DelegateExecution context, CloudControllerClient client,
                                                             List<String> serviceNames) {
        Map<String, ServiceOperationType> triggeredServiceOperations = new HashMap<>();

        for (String serviceName : serviceNames) {
            try {
                prepareServicesToDelete(client, serviceName);
                deleteService(client, serviceName);
                triggeredServiceOperations.put(serviceName, ServiceOperationType.DELETE);
            } catch (CloudException e) {
                processException(context, e, client.getServiceInstance(serviceName), serviceName);
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
            CloudApplication application = StepsUtil.getBoundApplication(client.getApplications(), binding.getApplicationGuid());
            if (application == null) {
                throw new IllegalStateException(MessageFormat.format(Messages.COULD_NOT_FIND_APPLICATION_WITH_GUID_0,
                                                                     binding.getApplicationGuid()));
            }
            getStepLogger().info(Messages.UNBINDING_APP_FROM_SERVICE, application.getName(), serviceName);
            client.unbindService(application.getName(), serviceName);
        }
    }

    private void deleteServiceKeys(CloudControllerClient client, String serviceName) {
        CloudService service = client.getService(serviceName);
        if (service.isUserProvided()) {
            return;
        }
        List<CloudServiceKey> serviceKeys = client.getServiceKeys(serviceName);
        for (CloudServiceKey serviceKey : serviceKeys) {
            getStepLogger().info(Messages.DELETING_SERVICE_KEY_FOR_SERVICE, serviceKey.getName(), serviceName);
            client.deleteServiceKey(serviceName, serviceKey.getName());
        }
    }

    private void deleteService(CloudControllerClient client, String serviceName) {
        getStepLogger().info(Messages.DELETING_SERVICE, serviceName);
        client.deleteService(serviceName);
        getStepLogger().debug(Messages.SERVICE_DELETED, serviceName);
    }

    private void processException(DelegateExecution context, Exception e, CloudServiceInstance serviceInstance, String serviceName) {
        if (e instanceof CloudOperationException) {
            e = evaluateCloudOperationException(context, (CloudOperationException) e, serviceName, serviceInstance.getService()
                                                                                                                  .getLabel());
            if (e == null) {
                return;
            }
        }
        wrapAndThrowException(e, serviceInstance, serviceName);
    }

    private CloudOperationException evaluateCloudOperationException(DelegateExecution context, CloudOperationException e,
                                                                    String serviceName, String label) {
        if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
            getStepLogger().warn(MessageFormat.format(Messages.COULD_NOT_DELETE_SERVICE, serviceName), e,
                                 ExceptionMessageTailMapper.map(configuration, CloudComponents.SERVICE_BROKERS, label));
            return null;
        }
        if (e.getStatusCode() == HttpStatus.BAD_GATEWAY) {
            StepsUtil.setServiceOffering(context, Constants.VAR_SERVICE_OFFERING, label);
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
        return Arrays.asList(new PollServiceDeleteOperationsExecution(serviceOperationGetter, serviceProgressReporter));
    }

}
