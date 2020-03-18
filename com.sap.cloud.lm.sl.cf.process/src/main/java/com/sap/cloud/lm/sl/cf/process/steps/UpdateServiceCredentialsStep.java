package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceUpdater;
import com.sap.cloud.lm.sl.cf.core.model.ServiceOperation;
import com.sap.cloud.lm.sl.cf.core.util.MethodExecution;
import com.sap.cloud.lm.sl.cf.process.Messages;

@Named("updateServiceCredentialsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateServiceCredentialsStep extends ServiceStep {

    @Inject
    @Named("serviceUpdater")
    protected ServiceUpdater serviceUpdater;

    @Override
    protected MethodExecution<String> executeOperation(ExecutionWrapper execution, CloudControllerClient controllerClient,
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
        return Collections.singletonList(new PollServiceCreateOrUpdateOperationsExecution(getServiceOperationGetter(),
                                                                                          getServiceProgressReporter()));
    }

    @Override
    protected ServiceOperation.Type getOperationType() {
        return ServiceOperation.Type.UPDATE;
    }
}
