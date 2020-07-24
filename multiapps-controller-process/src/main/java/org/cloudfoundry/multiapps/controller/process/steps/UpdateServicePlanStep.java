package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.model.ServiceOperation;
import org.cloudfoundry.multiapps.controller.core.util.MethodExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("updateServicePlanStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateServicePlanStep extends ServiceStep {

    @Override
    protected MethodExecution<String> executeOperation(ProcessContext context, CloudControllerClient controllerClient,
                                                       CloudServiceInstanceExtended service) {
        return updateServicePlan(controllerClient, service);
    }

    private MethodExecution<String> updateServicePlan(CloudControllerClient controllerClient, CloudServiceInstanceExtended service) {
        getStepLogger().debug(MessageFormat.format("Updating service plan of a service {0} with new plan: {1}", service.getName(),
                                                   service.getPlan()));
        if (service.shouldIgnoreUpdateErrors()) {
            return getServiceUpdater().updateServicePlanQuietly(controllerClient, service.getName(), service.getPlan());
        }
        return getServiceUpdater().updateServicePlan(controllerClient, service.getName(), service.getPlan());
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
