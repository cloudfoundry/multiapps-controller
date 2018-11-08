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
        MethodExecution<String> methodExecution = new MethodExecution<String>(null, ExecutionState.FINISHED);

        try {
            if (service.isUserProvided()) {
                client.createUserProvidedService(service, service.getCredentials());
            } else {
                methodExecution = serviceCreatorFactory.createInstance(getStepLogger())
                    .createService(client, service, StepsUtil.getSpaceId(context));
            }
            getStepLogger().debug(Messages.SERVICE_CREATED, service.getName());
        } catch (CloudOperationException e) {
            processServiceCreationFailure(service, e);
        }
        
        return methodExecution;
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
