package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.Collections;
import java.util.List;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.util.MethodExecution;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.ServiceOperation;

@Named("updateServicePlanStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateServicePlanStep extends ServiceStep {

    @Override
    protected MethodExecution<String> executeOperation(ProcessContext context, CloudControllerClient client,
                                                       CloudServiceInstanceExtended service) {
        if (service.shouldSkipPlanUpdate()) {
            getStepLogger().warn(Messages.WILL_NOT_UPDATE_SERVICE_PLAN, service.getName());
            return new MethodExecution<>(null, MethodExecution.ExecutionState.FINISHED);
        }
        getStepLogger().debug(Messages.UPDATING_SERVICE_0_WITH_PLAN_1, service.getName(), service.getPlan());

        client.updateServicePlan(service.getName(), service.getPlan());

        getStepLogger().debug(Messages.SERVICE_PLAN_FOR_SERVICE_0_UPDATED, service.getName());
        return new MethodExecution<>(null, MethodExecution.ExecutionState.FINISHED);
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ProcessContext context) {
        return Collections.singletonList(new PollServiceCreateOrUpdateOperationsExecution(getServiceOperationGetter(),
                                                                                          getServiceProgressReporter()));
    }

    @Override
    protected ServiceOperation.Type getOperationType() {
        return ServiceOperation.Type.UPDATE;
    }
}
