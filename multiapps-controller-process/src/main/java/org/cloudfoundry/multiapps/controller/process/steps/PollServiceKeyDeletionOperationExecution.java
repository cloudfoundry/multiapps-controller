package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.function.Consumer;

import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

import com.sap.cloudfoundry.client.facade.domain.CloudAsyncJob;

public class PollServiceKeyDeletionOperationExecution extends PollServiceKeyOperationExecution {

    @Override
    protected Consumer<CloudAsyncJob> getOnErrorHandlerForOptionalResource(ProcessContext context) {
        String serviceInstanceToDelete = context.getVariable(Variables.SERVICE_TO_DELETE);
        return serviceKeyJob -> context.getStepLogger()
                                       .warn(Messages.ASYNC_OPERATION_FOR_OPTIONAL_SERVICE_KEY_FAILED_WITH, serviceInstanceToDelete,
                                             serviceKeyJob.getErrors());
    }

    @Override
    protected Consumer<CloudAsyncJob> getOnErrorHandler(ProcessContext context) {
        String serviceInstanceToDelete = context.getVariable(Variables.SERVICE_TO_DELETE);
        return serviceKeyJob -> context.getStepLogger()
                                       .error(Messages.ASYNC_OPERATION_FOR_SERVICE_KEY_FAILED_WITH, serviceInstanceToDelete,
                                              serviceKeyJob.getErrors());
    }

    @Override
    protected String getAsyncJobId(ProcessContext context) {
        return context.getVariable(Variables.SERVICE_KEY_DELETION_JOB_ID);
    }

}
