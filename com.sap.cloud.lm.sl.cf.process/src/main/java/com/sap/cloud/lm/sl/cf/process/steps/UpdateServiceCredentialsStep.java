package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceUpdater;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.core.exec.MethodExecution;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

@Component("updateServiceCredentialsStep")
public class UpdateServiceCredentialsStep extends ServiceStep {

    @Inject
    @Named("serviceUpdater")
    protected ServiceUpdater serviceUpdater;

    @Override
    protected MethodExecution<String> executeOperation(DelegateExecution context, CloudControllerClient controllerClient,
        CloudServiceExtended service) {
        return updateServiceCredentials(controllerClient, service);
    }

    private MethodExecution<String> updateServiceCredentials(CloudControllerClient client, CloudServiceExtended service) {
        getStepLogger().info(Messages.UPDATING_SERVICE, service.getName());
        MethodExecution<String> methodExecution = updateService(client, service);
        getStepLogger().debug(Messages.SERVICE_UPDATED, service.getName());
        return methodExecution;
    }

    private MethodExecution<String> updateService(CloudControllerClient client, CloudServiceExtended service) {
        if (service.shouldIgnoreUpdateErrors()) {
            return serviceUpdater.updateServiceParametersQuietly(client, service.getName(), service.getCredentials());
        }
        return serviceUpdater.updateServiceParameters(client, service.getName(), service.getCredentials());
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
