package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.util.OperationExecutionState;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.DynamicResolvableParametersContextUpdater;
import org.cloudfoundry.multiapps.controller.process.util.ExceptionMessageTailMapper;
import org.cloudfoundry.multiapps.controller.process.util.ExceptionMessageTailMapper.CloudComponents;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;
import com.sap.cloudfoundry.client.facade.domain.ServiceOperation;

@Named("createServiceStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CreateServiceStep extends ServiceStep {

    @Override
    protected OperationExecutionState executeOperation(ProcessContext context, CloudControllerClient controllerClient,
                                                       CloudServiceInstanceExtended serviceInstance) {
        getStepLogger().info(Messages.CREATING_SERVICE_FROM_MTA_RESOURCE, serviceInstance.getName(), serviceInstance.getResourceName());
        try {
            OperationExecutionState executionState = createCloudService(controllerClient, serviceInstance);
            getStepLogger().debug(Messages.SERVICE_CREATED, serviceInstance.getName());
            setServiceGuid(context, serviceInstance);
            return executionState;
        } catch (CloudOperationException e) {
            Optional<OperationExecutionState> operationExecutionState = getServiceInstanceStateIfCreated(controllerClient, serviceInstance,
                                                                                                         e);
            if (operationExecutionState.isPresent()) {
                return operationExecutionState.get();
            }
            String exceptionDescription = MessageFormat.format(Messages.COULD_NOT_CREATE_OPTIONAL_SERVICE, serviceInstance.getName());
            CloudOperationException cloudOperationException = new CloudOperationException(e.getStatusCode(),
                                                                                          e.getStatusText(),
                                                                                          exceptionDescription);

            processServiceActionFailure(context, serviceInstance, cloudOperationException);
        }
        return OperationExecutionState.FINISHED;
    }

    private OperationExecutionState createCloudService(CloudControllerClient client, CloudServiceInstanceExtended service) {
        if (service.isUserProvided()) {
            return createUserProvidedServiceInstance(client, service);
        }
        return createManagedServiceInstance(client, service);
    }

    private OperationExecutionState createUserProvidedServiceInstance(CloudControllerClient client, CloudServiceInstanceExtended service) {
        client.createUserProvidedServiceInstance(service);
        return OperationExecutionState.FINISHED;
    }

    private OperationExecutionState createManagedServiceInstance(CloudControllerClient client, CloudServiceInstanceExtended service) {
        Assert.notNull(service, "Service must not be null");
        Assert.notNull(service.getName(), "Service name must not be null");
        Assert.notNull(service.getLabel(), "Service label must not be null");
        Assert.notNull(service.getPlan(), "Service plan must not be null");
        client.createServiceInstance(service);
        return OperationExecutionState.EXECUTING;
    }

    private void setServiceGuid(ProcessContext context, CloudServiceInstanceExtended serviceInstance) {
        new DynamicResolvableParametersContextUpdater(context).updateServiceGuid(serviceInstance);
    }

    private Optional<OperationExecutionState> getServiceInstanceStateIfCreated(CloudControllerClient controllerClient,
                                                                               CloudServiceInstanceExtended serviceInstance,
                                                                               CloudOperationException e) {
        if (e.getStatusCode() != HttpStatus.UNPROCESSABLE_ENTITY) {
            return Optional.empty();
        }
        CloudServiceInstance existingServiceInstance = controllerClient.getServiceInstanceWithoutAuxiliaryContent(serviceInstance.getName(),
                                                                                                                  false);
        if (existingServiceInstance == null) {
            return Optional.empty();
        }
        getStepLogger().warn(Messages.SERVICE_INSTANCE_ALREADY_EXISTS, serviceInstance.getName());
        return mapServiceInstanceState(existingServiceInstance);
    }

    private Optional<OperationExecutionState> mapServiceInstanceState(CloudServiceInstance existingServiceInstance) {
        ServiceOperation.State state = existingServiceInstance.getLastOperation()
                                                              .getState();
        if (state == ServiceOperation.State.IN_PROGRESS || state == ServiceOperation.State.INITIAL) {
            return Optional.of(OperationExecutionState.EXECUTING);
        }
        if (state == ServiceOperation.State.SUCCEEDED) {
            return Optional.of(OperationExecutionState.FINISHED);
        }
        return Optional.empty();
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ProcessContext context) {
        return List.of(new PollServiceCreateOrUpdateOperationsExecution(getServiceOperationGetter(), getServiceProgressReporter()));
    }

    @Override
    protected ServiceOperation.Type getOperationType() {
        return ServiceOperation.Type.CREATE;
    }

    @Override
    protected String getStepErrorMessageAdditionalDescription(ProcessContext context) {
        String offering = context.getVariable(Variables.SERVICE_OFFERING);
        return ExceptionMessageTailMapper.map(configuration, CloudComponents.SERVICE_BROKERS, offering);
    }

}
