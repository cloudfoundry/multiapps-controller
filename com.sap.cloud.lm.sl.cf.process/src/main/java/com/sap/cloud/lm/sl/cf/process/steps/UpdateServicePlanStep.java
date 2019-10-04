package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.core.exec.MethodExecution;

@Named("updateServicePlanStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateServicePlanStep extends ServiceStep {

    @Override
    protected MethodExecution<String> executeOperation(DelegateExecution context, CloudControllerClient controllerClient,
                                                       CloudServiceExtended service) {
        return updateServicePlan(controllerClient, service);
    }

    private MethodExecution<String> updateServicePlan(CloudControllerClient client, CloudServiceExtended service) {
        getStepLogger().debug(MessageFormat.format("Updating service plan of a service {0} with new plan: {1}", service.getName(),
                                                   service.getPlan()));
        if (service.shouldIgnoreUpdateErrors()) {
            return getServiceUpdater().updateServicePlanQuietly(client, service.getName(), service.getPlan());
        }
        return getServiceUpdater().updateServicePlan(client, service.getName(), service.getPlan());
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ExecutionWrapper execution) {
        return Collections.singletonList(new PollServiceCreateOrUpdateOperationsExecution(getServiceOperationGetter(),
                                                                                          getServiceProgressReporter()));
    }

    @Override
    protected ServiceOperationType getOperationType() {
        return ServiceOperationType.UPDATE;
    }
}
