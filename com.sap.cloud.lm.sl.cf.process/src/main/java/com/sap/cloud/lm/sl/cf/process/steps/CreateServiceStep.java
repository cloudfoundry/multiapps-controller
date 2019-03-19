package com.sap.cloud.lm.sl.cf.process.steps;

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
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

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
        } catch (FileStorageException e) {
            throw new SLException(e, e.getMessage());
        }

        return new MethodExecution<>(null, ExecutionState.FINISHED);
    }

    private MethodExecution<String> createCloudService(DelegateExecution context, CloudControllerClient client,
        CloudServiceExtended service) throws FileStorageException {
        if (service.isUserProvided()) {
            client.createUserProvidedService(service, service.getCredentials());
            return new MethodExecution<>(null, ExecutionState.FINISHED);
        }
        return createManagedService(context, client, service);
    }

    private MethodExecution<String> createManagedService(DelegateExecution context, CloudControllerClient client,
        CloudServiceExtended service) throws FileStorageException {
        return serviceCreatorFactory.createInstance(getStepLogger())
            .createService(client, service, StepsUtil.getSpaceId(context));
    }


    private void processServiceCreationFailure(CloudServiceExtended service, CloudOperationException e) {
        if (!service.isOptional()) {
            if (e.getStatusCode() == HttpStatus.BAD_GATEWAY) {
                throw new CloudServiceBrokerException(e);
            }
            throw new CloudControllerException(e);
        }
        getStepLogger().warn(e, Messages.COULD_NOT_EXECUTE_OPERATION_OVER_OPTIONAL_SERVICE, service.getName());
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ExecutionWrapper execution) {
        return Arrays.asList(new PollServiceCreateOrUpdateOperationsExecution(getServiceInstanceGetter()));
    }

    @Override
    protected ServiceOperationType getOperationType() {
        return ServiceOperationType.CREATE;
    }

}
