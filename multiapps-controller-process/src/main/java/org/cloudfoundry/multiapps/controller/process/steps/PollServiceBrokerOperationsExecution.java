package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.function.Consumer;

import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

import com.sap.cloudfoundry.client.facade.domain.CloudAsyncJob;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBroker;

public class PollServiceBrokerOperationsExecution extends PollOperationBaseExecution {

    @Override
    public AsyncExecutionState execute(ProcessContext context) {
        CloudServiceBroker broker = context.getVariable(Variables.CREATED_OR_UPDATED_SERVICE_BROKER);
        context.getStepLogger()
               .debug(Messages.POLLING_ASYNC_OPERATION_SERVICE_BROKER, broker.getName());
        return super.execute(context);
    }

    @Override
    protected String getAsyncJobId(ProcessContext context) {
        return context.getVariable(Variables.SERVICE_BROKER_ASYNC_JOB_ID);
    }

    @Override
    protected Consumer<CloudAsyncJob> getInProgressHandler(ProcessContext context) {
        CloudServiceBroker broker = context.getVariable(Variables.CREATED_OR_UPDATED_SERVICE_BROKER);
        return serviceBrokerJob -> context.getStepLogger()
                                          .debug(Messages.ASYNC_OPERATION_SERVICE_BROKER_IN_STATE_WITH_WARNINGS, broker.getName(),
                                                 serviceBrokerJob.getState(), serviceBrokerJob.getWarnings());
    }

    @Override
    protected Consumer<CloudAsyncJob> getOnCompleteHandler(ProcessContext context) {
        CloudServiceBroker broker = context.getVariable(Variables.CREATED_OR_UPDATED_SERVICE_BROKER);
        return serviceBrokerJob -> context.getStepLogger()
                                          .info(Messages.ASYNC_OPERATION_FOR_SERVICE_BROKER_FINISHED, broker.getName());
    }

    @Override
    protected Consumer<CloudAsyncJob> getOnErrorHandler(ProcessContext context) {
        CloudServiceBroker broker = context.getVariable(Variables.CREATED_OR_UPDATED_SERVICE_BROKER);
        return serviceBrokerJob -> context.getStepLogger()
                                          .error(Messages.ASYNC_OPERATION_FOR_SERVICE_BROKER_FAILED_WITH, broker.getName(),
                                                 serviceBrokerJob.getErrors());
    }

    @Override
    protected Consumer<CloudAsyncJob> getOnErrorHandlerForOptionalResource(ProcessContext context) {
        return getOnErrorHandler(context);
    }

    @Override
    protected boolean isOptional(ProcessContext context) {
        return false;
    }

    @Override
    public String getPollingErrorMessage(ProcessContext context) {
        CloudServiceBroker broker = context.getVariable(Variables.CREATED_OR_UPDATED_SERVICE_BROKER);
        return MessageFormat.format(Messages.ERROR_POLLING_ASYNC_SERVICE_BROKER, broker.getName());
    }

}
