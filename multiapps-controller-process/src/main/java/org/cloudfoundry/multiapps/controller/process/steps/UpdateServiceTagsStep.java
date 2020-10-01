package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.Collections;
import java.util.List;

import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.ServiceOperation;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.util.MethodExecution;
import org.cloudfoundry.multiapps.controller.core.util.MethodExecution.ExecutionState;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("updateServiceTagsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateServiceTagsStep extends ServiceStep {

    @Override
    protected MethodExecution<String> executeOperation(ProcessContext context, CloudControllerClient controllerClient,
                                                       CloudServiceInstanceExtended service) {
        return updateServiceTags(controllerClient, service);
    }

    private MethodExecution<String> updateServiceTags(CloudControllerClient controllerClient, CloudServiceInstanceExtended service) {
        // TODO: Remove the service.isUserProvided() check when user provided services support tags.
        // See the following issue for more info:
        // https://www.pivotaltracker.com/n/projects/966314/stories/105674948
        if (service.isUserProvided()) {
            return new MethodExecution<>(null, ExecutionState.FINISHED);
        }
        getStepLogger().info(Messages.UPDATING_SERVICE_TAGS, service.getName());

        MethodExecution<String> methodExecution = updateService(controllerClient, service);

        getStepLogger().debug(Messages.SERVICE_TAGS_UPDATED, service.getName());
        return methodExecution;
    }

    private MethodExecution<String> updateService(CloudControllerClient client, CloudServiceInstanceExtended service) {
        if (service.shouldIgnoreUpdateErrors()) {
            return getServiceUpdater().updateServiceTagsQuietly(client, service.getName(), service.getTags());
        }
        return getServiceUpdater().updateServiceTags(client, service.getName(), service.getTags());
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
