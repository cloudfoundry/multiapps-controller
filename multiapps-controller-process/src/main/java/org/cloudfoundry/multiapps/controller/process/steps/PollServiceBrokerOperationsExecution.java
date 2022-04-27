package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;

import org.cloudfoundry.client.v3.jobs.JobState;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudAsyncJob;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBroker;

public class PollServiceBrokerOperationsExecution implements AsyncExecution {

    @Override
    public AsyncExecutionState execute(ProcessContext context) {
        CloudServiceBroker broker = context.getVariable(Variables.CREATED_OR_UPDATED_SERVICE_BROKER);
        String jobId = context.getVariable(Variables.SERVICE_BROKER_ASYNC_JOB_ID);

        context.getStepLogger()
               .debug(Messages.POLLING_ASYNC_OPERATION_SERVICE_BROKER, broker.getName());

        return checkAsyncJobStatus(context, broker.getName(), jobId);
    }

    protected AsyncExecutionState checkAsyncJobStatus(ProcessContext context, String serviceBrokerName, String jobId) {
        CloudControllerClient client = context.getControllerClient();
        CloudAsyncJob job = client.getAsyncJob(jobId);

        if (job.getState() == JobState.COMPLETE) {
            context.getStepLogger()
                   .info(Messages.ASYNC_OPERATION_FOR_SERVICE_BROKER_FINISHED, serviceBrokerName);
            return AsyncExecutionState.FINISHED;
        }

        if (job.getState() == JobState.FAILED) {
            context.getStepLogger()
                   .error(Messages.ASYNC_OPERATION_FOR_SERVICE_BROKER_FAILED_WITH, serviceBrokerName, job.getErrors());
            return AsyncExecutionState.ERROR;
        }

        context.getStepLogger()
               .debug(Messages.ASYNC_OPERATION_SERVICE_BROKER_IN_STATE_WITH_WARNINGS, serviceBrokerName, job.getState(), job.getWarnings());
        return AsyncExecutionState.RUNNING;
    }

    @Override
    public String getPollingErrorMessage(ProcessContext context) {
        CloudServiceBroker broker = context.getVariable(Variables.CREATED_OR_UPDATED_SERVICE_BROKER);
        return MessageFormat.format(Messages.ERROR_POLLING_ASYNC_SERVICE_BROKER, broker.getName());
    }

}
