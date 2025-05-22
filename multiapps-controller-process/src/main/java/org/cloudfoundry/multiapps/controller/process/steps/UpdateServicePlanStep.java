package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;
import java.util.function.Supplier;
import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.ServiceOperation;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.util.OperationExecutionState;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("updateServicePlanStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateServicePlanStep extends ServiceStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateServicePlanStep.class);

    @Override
    protected OperationExecutionState executeOperation(ProcessContext context, CloudControllerClient client,
                                                       CloudServiceInstanceExtended service) {
        if (service.shouldSkipPlanUpdate()) {
            getStepLogger().warn(Messages.WILL_NOT_UPDATE_SERVICE_PLAN, service.getName());
            return OperationExecutionState.FINISHED;
        }
        getStepLogger().debug(Messages.UPDATING_SERVICE_0_WITH_PLAN_1, service.getName(), service.getPlan());
        try {
            client.updateServicePlan(service.getName(), service.getPlan());
            getStepLogger().debug(Messages.SERVICE_PLAN_FOR_SERVICE_0_UPDATED, service.getName());
        } catch (CloudOperationException e) {
            if (service.shouldFailOnPlanUpdateFailure() != null && !service.shouldFailOnPlanUpdateFailure()) {
                getStepLogger().warn(
                    MessageFormat.format(Messages.SERVICE_INSTANCE_0_PLAN_UPDATE_FAILED_IGNORING_FAILURE, service.getName()));
                LOGGER.error(MessageFormat.format(Messages.SERVICE_INSTANCE_0_PLAN_UPDATE_FAILED_ERROR_1, service.getName(),
                                                  e.getMessage()), e);
                return OperationExecutionState.FINISHED;
            }
            String exceptionDescription = MessageFormat.format(Messages.COULD_NOT_UPDATE_PLAN_SERVICE, service.getName(),
                                                               e.getDescription());
            CloudOperationException cloudOperationException = new CloudOperationException(e.getStatusCode(),
                                                                                          e.getStatusText(),
                                                                                          exceptionDescription);
            processServiceActionFailure(context, service, cloudOperationException);
            return OperationExecutionState.FINISHED;
        }

        return OperationExecutionState.EXECUTING;
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ProcessContext context) {
        CloudServiceInstanceExtended serviceToProcess = context.getVariable(Variables.SERVICE_TO_PROCESS);
        return List.of(new PollServiceCreateOrUpdateOperationsExecution(getServiceOperationGetter(),
                                                                        getServiceProgressReporter(),
                                                                        shouldFailOnPlanUpdateFailure(
                                                                            serviceToProcess)));
    }

    private Supplier<Boolean> shouldFailOnPlanUpdateFailure(CloudServiceInstanceExtended serviceToProcess) {
        if (serviceToProcess.shouldFailOnPlanUpdateFailure() != null) {
            return serviceToProcess::shouldFailOnPlanUpdateFailure;
        }
        return () -> false;
    }

    @Override
    protected ServiceOperation.Type getOperationType() {
        return ServiceOperation.Type.UPDATE;
    }
}
