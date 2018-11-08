package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.core.exec.MethodExecution;

@Component("updateServicePlanStep")
public class UpdateServicePlanStep extends ServiceStep {

    @Override
    protected MethodExecution<String> executeOperation(DelegateExecution context, CloudControllerClient controllerClient,
        CloudServiceExtended service) {
        return updateServicePlan(controllerClient, service);
    }

    private MethodExecution<String> updateServicePlan(CloudControllerClient client, CloudServiceExtended service) {
        getStepLogger()
            .debug(MessageFormat.format("Updating service plan of a service {0} with new plan: {1}", service.getName(), service.getPlan()));
        if (service.shouldIgnoreUpdateErrors()) {
            return getServiceUpdater().updateServicePlanQuietly(client, service.getName(), service.getPlan());
        }
        return getServiceUpdater().updateServicePlan(client, service.getName(), service.getPlan());
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ExecutionWrapper execution) {
        return Arrays.asList(new PollServiceCreateOrUpdateOperationsExecution(getServiceInstanceGetter()));
    }

    @Override
    protected ServiceOperationType getOperationType() {
        return ServiceOperationType.UPDATE;
    }
}
