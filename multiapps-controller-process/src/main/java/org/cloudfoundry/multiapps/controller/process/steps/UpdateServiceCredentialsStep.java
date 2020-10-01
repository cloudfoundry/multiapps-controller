package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.ServiceOperation;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.cf.clients.ServiceUpdater;
import org.cloudfoundry.multiapps.controller.core.util.MethodExecution;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("updateServiceCredentialsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateServiceCredentialsStep extends ServiceStep {

    @Inject
    @Named("serviceUpdater")
    protected ServiceUpdater serviceUpdater;

    @Override
    protected MethodExecution<String> executeOperation(ProcessContext context, CloudControllerClient controllerClient,
                                                       CloudServiceInstanceExtended service) {
        return updateServiceCredentials(controllerClient, service);
    }

    private MethodExecution<String> updateServiceCredentials(CloudControllerClient client, CloudServiceInstanceExtended service) {
        getStepLogger().info(Messages.UPDATING_SERVICE, service.getName());
        MethodExecution<String> methodExecution = updateService(client, service);
        getStepLogger().debug(Messages.SERVICE_UPDATED, service.getName());
        return methodExecution;
    }

    private MethodExecution<String> updateService(CloudControllerClient client, CloudServiceInstanceExtended service) {
        if (service.shouldIgnoreUpdateErrors()) {
            return serviceUpdater.updateServiceParametersQuietly(client, service.getName(), service.getCredentials());
        }
        return serviceUpdater.updateServiceParameters(client, service.getName(), service.getCredentials());
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
