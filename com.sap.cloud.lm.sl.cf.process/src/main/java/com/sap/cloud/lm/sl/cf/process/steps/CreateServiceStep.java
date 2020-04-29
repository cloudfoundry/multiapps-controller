package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.CloudServiceBrokerException;
import org.cloudfoundry.client.lib.domain.CloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceInstance;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceInstanceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceWithAlternativesCreator;
import com.sap.cloud.lm.sl.cf.core.model.ServiceOperation;
import com.sap.cloud.lm.sl.cf.core.util.MethodExecution;
import com.sap.cloud.lm.sl.cf.core.util.MethodExecution.ExecutionState;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ExceptionMessageTailMapper;
import com.sap.cloud.lm.sl.cf.process.util.ExceptionMessageTailMapper.CloudComponents;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Named("createServiceStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CreateServiceStep extends ServiceStep {

    @Inject
    private ServiceWithAlternativesCreator.Factory serviceCreatorFactory;

    @Override
    protected MethodExecution<String> executeOperation(ProcessContext context, CloudControllerClient controllerClient,
                                                       CloudServiceInstanceExtended service) {
        return createService(context, controllerClient, service);
    }

    private MethodExecution<String> createService(ProcessContext context, CloudControllerClient controllerClient,
                                                  CloudServiceInstanceExtended service) {
        getStepLogger().info(Messages.CREATING_SERVICE_FROM_MTA_RESOURCE, service.getName(), service.getResourceName());

        try {
            MethodExecution<String> createServiceMethodExecution = createCloudService(controllerClient, service);
            getStepLogger().debug(Messages.SERVICE_CREATED, service.getName());
            return createServiceMethodExecution;
        } catch (CloudOperationException e) {
            processServiceCreationFailure(context, service, e);
        }

        return new MethodExecution<>(null, ExecutionState.FINISHED);
    }

    private MethodExecution<String> createCloudService(CloudControllerClient client, CloudServiceInstanceExtended service) {
        if (serviceExists(service, client)) {
            getStepLogger().info(com.sap.cloud.lm.sl.cf.core.Messages.SERVICE_ALREADY_EXISTS, service.getName());
            return new MethodExecution<>(null, ExecutionState.FINISHED);
        }
        if (service.isUserProvided()) {
            return createUserProvidedServiceInstance(client, service);
        }
        return createManagedServiceInstance(client, service);
    }

    private MethodExecution<String> createUserProvidedServiceInstance(CloudControllerClient client, CloudServiceInstanceExtended service) {
        client.createUserProvidedServiceInstance(service, service.getCredentials());
        return new MethodExecution<>(null, ExecutionState.FINISHED);
    }

    private boolean serviceExists(CloudServiceInstanceExtended cloudServiceExtended, CloudControllerClient client) {
        return client.getServiceInstance(cloudServiceExtended.getName(), false) != null;
    }

    private MethodExecution<String> createManagedServiceInstance(CloudControllerClient client, CloudServiceInstanceExtended service) {
        MethodExecution<String> createService = serviceCreatorFactory.createInstance(getStepLogger())
                                                                     .createService(client, service);
        updateServiceMetadata(service, client);
        return createService;
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
        getStepLogger().warn(MessageFormat.format(Messages.COULD_NOT_EXECUTE_OPERATION_OVER_OPTIONAL_SERVICE, service.getName()), e,
                             ExceptionMessageTailMapper.map(configuration, CloudComponents.SERVICE_BROKERS, service.getLabel()));
    }

    private void updateServiceMetadata(CloudServiceInstanceExtended serviceToProcess, CloudControllerClient client) {
        ImmutableCloudServiceInstance serviceWithMetadata = ImmutableCloudServiceInstance.copyOf(serviceToProcess);
        CloudMetadata serviceMeta = client.getServiceInstance(serviceWithMetadata.getName())
                                          .getMetadata();
        serviceWithMetadata = serviceWithMetadata.withMetadata(serviceMeta);
        client.updateServiceInstanceMetadata(serviceWithMetadata.getMetadata()
                                                                .getGuid(),
                                             serviceWithMetadata.getV3Metadata());
        getStepLogger().debug("updated service metadata name: " + serviceWithMetadata + " metadata: "
            + JsonUtil.toJson(serviceWithMetadata.getV3Metadata(), true));
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ProcessContext context) {
        return Collections.singletonList(new PollServiceCreateOrUpdateOperationsExecution(getServiceOperationGetter(),
                                                                                          getServiceProgressReporter()));
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
