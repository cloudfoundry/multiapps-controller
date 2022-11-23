package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.function.Consumer;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

import com.sap.cloudfoundry.client.facade.domain.CloudAsyncJob;

public class PollServiceKeyCreationOperationExecution extends PollServiceKeyOperationExecution {

    @Override
    protected Consumer<CloudAsyncJob> getOnErrorHandlerForOptionalResource(ProcessContext context) {
        CloudServiceInstanceExtended serviceInstanceToProcess = context.getVariable(Variables.SERVICE_TO_PROCESS);
        return serviceKeyJob -> context.getStepLogger()
                                       .warn(Messages.ASYNC_OPERATION_FOR_OPTIONAL_SERVICE_KEY_FAILED_WITH,
                                              serviceInstanceToProcess.getName(), serviceKeyJob.getErrors());
    }

    @Override
    protected Consumer<CloudAsyncJob> getOnErrorHandler(ProcessContext context) {
        CloudServiceInstanceExtended serviceInstanceToProcess = context.getVariable(Variables.SERVICE_TO_PROCESS);
        return serviceKeyJob -> context.getStepLogger()
                                       .error(Messages.ASYNC_OPERATION_FOR_SERVICE_KEY_FAILED_WITH, serviceInstanceToProcess.getName(),
                                              serviceKeyJob.getErrors());
    }

    @Override
    protected String getAsyncJobId(ProcessContext context) {
        return context.getVariable(Variables.SERVICE_KEY_CREATION_JOB_ID);
    }

}
