package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.CloudServiceBrokerException;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceWithAlternativesCreator;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.core.exec.MethodExecution;
import com.sap.cloud.lm.sl.cf.core.exec.MethodExecution.ExecutionState;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

@Component("createServiceStep")
public class CreateServiceStep extends ServiceStep {

    @Inject
    private ServiceWithAlternativesCreator.Factory serviceCreatorFactory;

    @Override
    protected MethodExecution<String> executeOperation(DelegateExecution execution, CloudControllerClient controllerClient,
                                                       CloudServiceExtended service) {
        return createService(execution, controllerClient, service);
    }

    private MethodExecution<String> createService(DelegateExecution context, CloudControllerClient client, CloudServiceExtended service) {
        getStepLogger().info(Messages.CREATING_SERVICE_FROM_MTA_RESOURCE, service.getName(), service.getResourceName());

        try {
            MethodExecution<String> createServiceMethodExecution = createCloudService(context, client, service);
            getStepLogger().debug(Messages.SERVICE_CREATED, service.getName());
            return createServiceMethodExecution;
        } catch (CloudOperationException e) {
            processServiceCreationFailure(service, e);
        }

        return new MethodExecution<>(null, ExecutionState.FINISHED);
    }

    private MethodExecution<String> createCloudService(DelegateExecution context, CloudControllerClient client,
                                                       CloudServiceExtended service) {

        if (serviceExists(service, client)) {
            getStepLogger().info(com.sap.cloud.lm.sl.cf.core.message.Messages.SERVICE_ALREADY_EXISTS, service.getName());
            return new MethodExecution<>(null, ExecutionState.FINISHED);
        }
        if (service.isUserProvided()) {
            return createUserProvidedService(client, service);
        }
        return createManagedService(context, client, service);
    }

    private MethodExecution<String> createUserProvidedService(CloudControllerClient client, CloudServiceExtended service) {
        client.createUserProvidedService(service, service.getCredentials());
        return new MethodExecution<>(null, ExecutionState.FINISHED);
    }

    private boolean serviceExists(CloudServiceExtended cloudServiceExtended, CloudControllerClient client) {
        return client.getService(cloudServiceExtended.getName(), false) != null;
    }

    private MethodExecution<String> createManagedService(DelegateExecution context, CloudControllerClient client,
                                                         CloudServiceExtended service) {
        return serviceCreatorFactory.createInstance(getStepLogger())
                                    .createService(client, service, StepsUtil.getSpaceId(context));
    }

    private void processServiceCreationFailure(CloudServiceExtended service, CloudOperationException e) {
        if (!service.isOptional()) {
            String detailedDescription = MessageFormat.format(Messages.ERROR_CREATING_SERVICE, service.getName(), service.getLabel(),
                                                              service.getPlan(), e.getDescription());
            if (e.getStatusCode() == HttpStatus.BAD_GATEWAY) {
                throw new CloudServiceBrokerException(e.getStatusCode(), e.getStatusText(), detailedDescription);
            }
            throw new CloudControllerException(e.getStatusCode(), e.getStatusText(), detailedDescription);
        }
        getStepLogger().warn(e, Messages.COULD_NOT_EXECUTE_OPERATION_OVER_OPTIONAL_SERVICE, service.getName());
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ExecutionWrapper execution) {
        return Arrays.asList(new PollServiceCreateOrUpdateOperationsExecution(getServiceOperationGetter(), getServiceProgressReporter()));
    }

    @Override
    protected ServiceOperationType getOperationType() {
        return ServiceOperationType.CREATE;
    }

}
