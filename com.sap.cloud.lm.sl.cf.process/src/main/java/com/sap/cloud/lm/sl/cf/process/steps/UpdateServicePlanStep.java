package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.model.ServiceOperation;
import com.sap.cloud.lm.sl.cf.core.util.MethodExecution;

@Named("updateServicePlanStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateServicePlanStep extends ServiceStep {

    @Override
    protected MethodExecution<String> executeOperation(ProcessContext context, CloudControllerClient controllerClient,
                                                       CloudServiceExtended service) {
        return updateServicePlan(controllerClient, service);
    }

    private MethodExecution<String> updateServicePlan(CloudControllerClient controllerClient, CloudServiceExtended service) {
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
