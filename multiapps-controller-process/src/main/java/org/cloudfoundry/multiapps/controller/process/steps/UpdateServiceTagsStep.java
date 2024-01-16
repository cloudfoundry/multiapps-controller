package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.util.OperationExecutionState;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.ServiceOperation;

@Named("updateServiceTagsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateServiceTagsStep extends ServiceStep {

    @Override
    protected OperationExecutionState executeOperation(ProcessContext context, CloudControllerClient client,
                                                       CloudServiceInstanceExtended service) {
        if (service.shouldSkipTagsUpdate()) {
            getStepLogger().warn(Messages.WILL_NOT_UPDATE_SERVICE_TAGS, service.getName());
            return OperationExecutionState.FINISHED;
        }
        getStepLogger().info(Messages.UPDATING_SERVICE_TAGS, service.getName());

        try {
            client.updateServiceTags(service.getName(), service.getTags());
            getStepLogger().debug(Messages.SERVICE_TAGS_UPDATED, service.getName());
        } catch (CloudOperationException e) {
            String exceptionDescription = MessageFormat.format(Messages.COULD_NOT_UPDATE_TAGS_OF_OPTIONAL_SERVICE, service.getName(),
                                                               e.getDescription());
            CloudOperationException cloudOperationException = new CloudOperationException(e.getStatusCode(),
                                                                                          e.getStatusText(),
                                                                                          exceptionDescription);

            processServiceActionFailure(context, service, cloudOperationException);
            return OperationExecutionState.FINISHED;
        }

        return OperationExecutionState.EXECUTING;
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
