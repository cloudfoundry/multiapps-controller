package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.AsyncJobToAsyncExecutionStateAdapter;
import org.cloudfoundry.multiapps.controller.process.util.ImmutableAsyncJobToAsyncExecutionStateAdapter;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudAsyncJob;

public class PollServiceBrokersOperationsExecution implements AsyncExecution {

    @Override
    public AsyncExecutionState execute(ProcessContext context) {
        Map<String, String> serviceBrokerNamesJobIds = context.getVariable(Variables.SERVICE_BROKER_NAMES_JOB_IDS);

        Map<String, String> serviceBrokerStillInProgress = new HashMap<>();
        for (var brokerNameWithJobId : serviceBrokerNamesJobIds.entrySet()) {
            String serviceBrokerName = brokerNameWithJobId.getKey();
            String jobId = brokerNameWithJobId.getValue();
            CloudControllerClient controllerClient = context.getControllerClient();
            AsyncExecutionState asyncJobStatus = createAsyncJobAdapter(context,
                                                                       serviceBrokerName).evaluateState(controllerClient.getAsyncJob(jobId));
            if (asyncJobStatus == AsyncExecutionState.ERROR) {
                return AsyncExecutionState.ERROR;
            }
            if (asyncJobStatus != AsyncExecutionState.FINISHED) {
                serviceBrokerStillInProgress.put(serviceBrokerName, jobId);
            }
        }

        if (serviceBrokerStillInProgress.isEmpty()) {
            return AsyncExecutionState.FINISHED;
        }

        context.setVariable(Variables.SERVICE_BROKER_NAMES_JOB_IDS, serviceBrokerStillInProgress);
        return AsyncExecutionState.RUNNING;
    }

    private AsyncJobToAsyncExecutionStateAdapter createAsyncJobAdapter(ProcessContext context, String serviceBrokerName) {
        return ImmutableAsyncJobToAsyncExecutionStateAdapter.builder()
                                                            .inProgressHandler(getInProgressHandler(context, serviceBrokerName))
                                                            .onCompleteHandler(getOnCompleteHandler(context, serviceBrokerName))
                                                            .onErrorHandler(getOnErrorHandler(context, serviceBrokerName))
                                                            .onErrorHandlerForOptionalResource(getOnErrorHandlerForOptionalResource(context,
                                                                                                                                    serviceBrokerName))
                                                            .build();
    }

    private Consumer<CloudAsyncJob> getInProgressHandler(ProcessContext context, String serviceBrokerName) {
        return serviceBrokerJob -> context.getStepLogger()
                                          .debug(Messages.ASYNC_OPERATION_SERVICE_BROKER_IN_STATE_WITH_WARNINGS, serviceBrokerName,
                                                 serviceBrokerJob.getState(), serviceBrokerJob.getWarnings());

    }

    private Consumer<CloudAsyncJob> getOnCompleteHandler(ProcessContext context, String serviceBrokerName) {
        return serviceBrokerJob -> context.getStepLogger()
                                          .info(Messages.ASYNC_OPERATION_FOR_SERVICE_BROKER_FINISHED, serviceBrokerName);

    }

    private Consumer<CloudAsyncJob> getOnErrorHandler(ProcessContext context, String serviceBrokerName) {
        return serviceBrokerJob -> context.getStepLogger()
                                          .error(Messages.ASYNC_OPERATION_FOR_SERVICE_BROKER_FAILED_WITH, serviceBrokerName,
                                                 serviceBrokerJob.getErrors());
    }

    private Consumer<CloudAsyncJob> getOnErrorHandlerForOptionalResource(ProcessContext context, String serviceBrokerName) {
        return getOnErrorHandler(context, serviceBrokerName);
    }

    @Override
    public String getPollingErrorMessage(ProcessContext context) {
        return Messages.ERROR_POLLING_ASYNC_SERVICE_BROKERS;
    }

}
