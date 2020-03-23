package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
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
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.model.ServiceOperation;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ExceptionMessageTailMapper;
import com.sap.cloud.lm.sl.cf.process.util.ExceptionMessageTailMapper.CloudComponents;
import com.sap.cloud.lm.sl.cf.process.util.ServiceOperationGetter;
import com.sap.cloud.lm.sl.cf.process.util.ServiceProgressReporter;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

/**
 * 
 * @deprecated This class should be deleted after release of new version with new step {@link DeleteServiceStep}. This step will be used
 *             only with old running flowable processes
 */
@Named("deleteServicesStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Deprecated
public class DeleteServicesStep extends AsyncFlowableStep {

    private final SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    private ServiceOperationGetter serviceOperationGetter;
    private ServiceProgressReporter serviceProgressReporter;

    @Inject
    public DeleteServicesStep(ServiceOperationGetter serviceOperationGetter, ServiceProgressReporter serviceProgressReporter) {
        this.serviceOperationGetter = serviceOperationGetter;
        this.serviceProgressReporter = serviceProgressReporter;
    }

    @Override
    protected StepPhase executeAsyncStep(ProcessContext context) {
        getStepLogger().debug(Messages.DELETING_SERVICES);

        CloudControllerClient client = context.getControllerClient();

        List<String> servicesToDelete = new ArrayList<>(StepsUtil.getServicesToDelete(context.getExecution()));

        if (servicesToDelete.isEmpty()) {
            getStepLogger().debug(Messages.MISSING_SERVICES_TO_DELETE);
            return StepPhase.DONE;
        }

        List<CloudServiceExtended> servicesData = getServicesData(servicesToDelete, context);
        List<String> servicesWithoutData = getServicesWithoutData(servicesToDelete, servicesData);
        if (!servicesWithoutData.isEmpty()) {
            context.getStepLogger()
                   .info(Messages.SERVICES_ARE_ALREADY_DELETED, servicesWithoutData);
            servicesToDelete.removeAll(servicesWithoutData);
        }
        StepsUtil.setServicesData(context.getExecution(), servicesData);

        Map<String, ServiceOperation.Type> triggeredServiceOperations = deleteServices(context, client, servicesToDelete);

        context.getStepLogger()
               .debug(Messages.TRIGGERED_SERVICE_OPERATIONS, JsonUtil.toJson(triggeredServiceOperations, true));
        context.setVariable(Variables.TRIGGERED_SERVICE_OPERATIONS, triggeredServiceOperations);

        getStepLogger().debug(Messages.SERVICES_DELETED);
        return StepPhase.POLL;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_DELETING_SERVICES;
    }

    @Override
    protected String getStepErrorMessageAdditionalDescription(ProcessContext context) {
        String offering = context.getVariable(Variables.SERVICE_OFFERING);
        return ExceptionMessageTailMapper.map(configuration, CloudComponents.SERVICE_BROKERS, offering);
    }

    private List<CloudServiceExtended> getServicesData(List<String> serviceNames, ProcessContext context) {
        CloudControllerClient client = context.getControllerClient();

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

    private Map<String, ServiceOperation.Type> deleteServices(ProcessContext context, CloudControllerClient client,
                                                              List<String> serviceNames) {
        Map<String, ServiceOperation.Type> triggeredServiceOperations = new HashMap<>();

        for (String serviceName : serviceNames) {
            try {
                prepareServicesToDelete(client, serviceName);
                deleteService(client, serviceName);
                triggeredServiceOperations.put(serviceName, ServiceOperation.Type.DELETE);
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
            getStepLogger().info(Messages.UNBINDING_SERVICE_FROM_APP, serviceName, application.getName());
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

    private void processException(ProcessContext context, Exception e, CloudServiceInstance serviceInstance, String serviceName) {
        if (e instanceof CloudOperationException) {
            e = evaluateCloudOperationException(context, (CloudOperationException) e, serviceName, serviceInstance.getService()
                                                                                                                  .getLabel());
            if (e == null) {
                return;
            }
        }
        wrapAndThrowException(e, serviceInstance, serviceName);
    }

    private CloudOperationException evaluateCloudOperationException(ProcessContext context, CloudOperationException e, String serviceName,
                                                                    String label) {
        if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
            getStepLogger().warn(MessageFormat.format(Messages.COULD_NOT_DELETE_SERVICE, serviceName), e,
                                 ExceptionMessageTailMapper.map(configuration, CloudComponents.SERVICE_BROKERS, label));
            return null;
        }
        if (e.getStatusCode() == HttpStatus.BAD_GATEWAY) {
            context.setVariable(Variables.SERVICE_OFFERING, label);
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
    protected List<AsyncExecution> getAsyncStepExecutions(ProcessContext context) {
        return Collections.singletonList(new PollServiceDeleteOperationsExecution(serviceOperationGetter, serviceProgressReporter));
    }

}