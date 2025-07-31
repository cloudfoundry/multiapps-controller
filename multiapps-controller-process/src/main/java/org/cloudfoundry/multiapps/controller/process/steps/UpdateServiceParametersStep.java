package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;
import java.util.function.Supplier;

import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceOperation;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.util.OperationExecutionState;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("updateServiceParametersStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateServiceParametersStep extends ServiceStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateServiceParametersStep.class);

    @Override
    protected OperationExecutionState executeOperation(ProcessContext context, CloudControllerClient client,
                                                       CloudServiceInstanceExtended service) {
        getStepLogger().info(Messages.UPDATING_SERVICE, service.getName());

        try {
            client.updateServiceParameters(service.getName(), service.getCredentials());
            getStepLogger().debug(Messages.SERVICE_UPDATED, service.getName());
        } catch (CloudOperationException e) {
            if (service.shouldFailOnParametersUpdateFailure() != null && !service.shouldFailOnParametersUpdateFailure()) {
                getStepLogger().warn(
                    MessageFormat.format(Messages.SERVICE_INSTANCE_0_PARAMETERS_UPDATE_FAILED_IGNORING_FAILURE, service.getName()));
                LOGGER.error(
                    MessageFormat.format(Messages.SERVICE_INSTANCE_0_PARAMETERS_UPDATE_FAILED_ERROR_1, service.getName(), e.getMessage()),
                    e);
                return OperationExecutionState.FINISHED;
            }
            String exceptionDescription = MessageFormat.format(Messages.COULD_NOT_UPDATE_PARAMETERS_SERVICE, service.getName(),
                                                               e.getDescription());
            CloudOperationException cloudOperationException = new CloudOperationException(e.getStatusCode(), e.getStatusText(),
                                                                                          exceptionDescription);
            processServiceActionFailure(context, service, cloudOperationException);
            return OperationExecutionState.FINISHED;
        }

        return OperationExecutionState.EXECUTING;
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ProcessContext context) {
        CloudServiceInstanceExtended serviceToProcess = context.getVariable(Variables.SERVICE_TO_PROCESS);
        return List.of(new PollServiceCreateOrUpdateOperationsExecution(getServiceOperationGetter(), getServiceProgressReporter(),
                                                                        shouldFailOnParametersUpdateFailure(serviceToProcess)));
    }

    private Supplier<Boolean> shouldFailOnParametersUpdateFailure(CloudServiceInstanceExtended serviceToProcess) {
        if (serviceToProcess.shouldFailOnParametersUpdateFailure() != null) {
            return serviceToProcess::shouldFailOnParametersUpdateFailure;
        }
        return () -> false;
    }

    @Override
    protected ServiceOperation.Type getOperationType() {
        return ServiceOperation.Type.UPDATE;
    }
}
