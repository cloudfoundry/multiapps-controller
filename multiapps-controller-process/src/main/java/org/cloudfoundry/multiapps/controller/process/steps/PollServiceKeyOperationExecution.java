package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.function.Consumer;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

import com.sap.cloudfoundry.client.facade.domain.CloudAsyncJob;

public abstract class PollServiceKeyOperationExecution extends PollOperationBaseExecution {

    @Override
    protected boolean isOptional(ProcessContext context) {
        CloudServiceInstanceExtended serviceToProcess = context.getVariable(Variables.SERVICE_TO_PROCESS);
        return serviceToProcess != null && serviceToProcess.isOptional();
    }

    @Override
    protected Consumer<CloudAsyncJob> getInProgressHandler(ProcessContext context) {
        return serviceKeyJob -> context.getStepLogger()
                                       .debug(Messages.ASYNC_OPERATION_SERVICE_KEY_IN_STATE_WITH_WARNINGS, serviceKeyJob.getState(),
                                              serviceKeyJob.getWarnings());
    }

    @Override
    protected Consumer<CloudAsyncJob> getOnCompleteHandler(ProcessContext context) {
        return serviceKeyJob -> context.getStepLogger()
                                       .debug(Messages.ASYNC_OPERATION_FOR_SERVICE_KEY_FINISHED, serviceKeyJob.getGuid());
    }

    @Override
    public String getPollingErrorMessage(ProcessContext context) {
        return Messages.ERROR_POLLING_ASYNC_SERVICE_KEY;
    }

}
