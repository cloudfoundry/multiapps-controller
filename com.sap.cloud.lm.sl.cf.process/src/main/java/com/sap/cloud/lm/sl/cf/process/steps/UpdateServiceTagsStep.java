package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Arrays;
import java.util.List;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.core.exec.MethodExecution;
import com.sap.cloud.lm.sl.cf.core.exec.MethodExecution.ExecutionState;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

@Component("updateServiceTagsStep")
public class UpdateServiceTagsStep extends ServiceStep {

    @Override
    protected MethodExecution<String> executeOperation(DelegateExecution context, CloudControllerClient controllerClient,
        CloudServiceExtended service) {
        return updateServiceTags(controllerClient, service);
    }

    private MethodExecution<String> updateServiceTags(CloudControllerClient client, CloudServiceExtended service) {
        // TODO: Remove the service.isUserProvided() check when user provided services support tags.
        // See the following issue for more info:
        // https://www.pivotaltracker.com/n/projects/966314/stories/105674948
        if (service.isUserProvided()) {
            return new MethodExecution<String>(null, ExecutionState.FINISHED);
        }
        getStepLogger().info(Messages.UPDATING_SERVICE_TAGS, service.getName());

        MethodExecution<String> methodExecution = updateService(client, service);

        getStepLogger().debug(Messages.SERVICE_TAGS_UPDATED, service.getName());
        return methodExecution;
    }

    private MethodExecution<String> updateService(CloudControllerClient client, CloudServiceExtended service) {
        if (service.shouldIgnoreUpdateErrors()) {
            return getServiceUpdater().updateServiceTagsQuietly(client, service.getName(), service.getTags());
        }
        return getServiceUpdater().updateServiceTags(client, service.getName(), service.getTags());
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
