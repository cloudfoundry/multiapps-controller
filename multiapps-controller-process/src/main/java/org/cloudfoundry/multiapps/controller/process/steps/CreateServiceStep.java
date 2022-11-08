package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.util.OperationExecutionState;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ExceptionMessageTailMapper;
import org.cloudfoundry.multiapps.controller.process.util.ExceptionMessageTailMapper.CloudComponents;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudControllerException;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.CloudServiceBrokerException;
import com.sap.cloudfoundry.client.facade.domain.ServiceOperation;

@Named("createServiceStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CreateServiceStep extends ServiceStep {

    @Override
    protected OperationExecutionState executeOperation(ProcessContext context, CloudControllerClient controllerClient,
                                                       CloudServiceInstanceExtended service) {
        getStepLogger().info(Messages.CREATING_SERVICE_FROM_MTA_RESOURCE, service.getName(), service.getResourceName());

        try {
            OperationExecutionState executionState = createCloudService(controllerClient, service);
            getStepLogger().debug(Messages.SERVICE_CREATED, service.getName());
            return executionState;
        } catch (CloudOperationException e) {
            processServiceCreationFailure(context, service, e);
        }

        return OperationExecutionState.FINISHED;
    }

    private OperationExecutionState createCloudService(CloudControllerClient client, CloudServiceInstanceExtended service) {
        if (serviceExists(service, client)) {
            getStepLogger().info(org.cloudfoundry.multiapps.controller.core.Messages.SERVICE_ALREADY_EXISTS, service.getName());
            return OperationExecutionState.FINISHED;
        }
        if (service.isUserProvided()) {
            return createUserProvidedServiceInstance(client, service);
        }
        return createManagedServiceInstance(client, service);
    }

    private OperationExecutionState createUserProvidedServiceInstance(CloudControllerClient client, CloudServiceInstanceExtended service) {
        client.createUserProvidedServiceInstance(service);
        return OperationExecutionState.FINISHED;
    }

    private boolean serviceExists(CloudServiceInstanceExtended cloudServiceExtended, CloudControllerClient client) {
        try {
            client.getRequiredServiceInstanceGuid(cloudServiceExtended.getName());
            return true;
        } catch (CloudOperationException e) {
            return false;
        }
    }

    private OperationExecutionState createManagedServiceInstance(CloudControllerClient client, CloudServiceInstanceExtended service) {
        Assert.notNull(service, "Service must not be null");
        Assert.notNull(service.getName(), "Service name must not be null");
        Assert.notNull(service.getLabel(), "Service label must not be null");
        Assert.notNull(service.getPlan(), "Service plan must not be null");
        client.createServiceInstance(service);
        return OperationExecutionState.EXECUTING;
    }

    private void processServiceCreationFailure(ProcessContext context, CloudServiceInstanceExtended service, CloudOperationException e) {
        if (!service.isOptional()) {
            String detailedDescription = MessageFormat.format(Messages.ERROR_CREATING_SERVICE, service.getName(), service.getLabel(),
                                                              service.getPlan(), e.getDescription());
            if (e.getStatusCode() == HttpStatus.BAD_GATEWAY) {
                context.setVariable(Variables.SERVICE_OFFERING, service.getLabel());
                throw new CloudServiceBrokerException(e.getStatusCode(), e.getStatusText(), detailedDescription);
            }
            throw new CloudControllerException(e.getStatusCode(), e.getStatusText(), detailedDescription);
        }
        getStepLogger().warn(MessageFormat.format(Messages.COULD_NOT_CREATE_OPTIONAL_SERVICE, service.getName()), e,
                             ExceptionMessageTailMapper.map(configuration, CloudComponents.SERVICE_BROKERS, service.getLabel()));
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ProcessContext context) {
        // The order is important. The metadata update should be done after the async creation of the service instance is done.
        // TODO: Add link to cloud_controller_ng issue.
        return Arrays.asList(new PollServiceCreateOrUpdateOperationsExecution(getServiceOperationGetter(), getServiceProgressReporter()),
                             new UpdateServiceMetadataExecution());
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
