package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.Collections;
import java.util.List;

import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.ServiceOperation;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.util.MethodExecution;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("updateServiceParametersStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateServiceParametersStep extends ServiceStep {

    @Override
    protected MethodExecution<String> executeOperation(ProcessContext context, CloudControllerClient client,
                                                       CloudServiceInstanceExtended service) {
        if (service.shouldSkipParametersUpdate()) {
            getStepLogger().warn(Messages.WILL_NOT_UPDATE_SERVICE_PARAMS, service.getName());
            return new MethodExecution<>(null, MethodExecution.ExecutionState.FINISHED);
        }
        getStepLogger().info(Messages.UPDATING_SERVICE, service.getName());

        CloudServiceInstance serviceInstance = client.getServiceInstance(service.getName());
        client.updateServiceParameters(serviceInstance);

        getStepLogger().debug(Messages.SERVICE_UPDATED, service.getName());
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
