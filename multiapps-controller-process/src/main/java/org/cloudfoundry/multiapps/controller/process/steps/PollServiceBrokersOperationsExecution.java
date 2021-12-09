package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

public class PollServiceBrokersOperationsExecution extends PollServiceBrokerOperationsExecution {

    @Override
    public AsyncExecutionState execute(ProcessContext context) {
        Map<String, String> serviceBrokerNamesJobIds = context.getVariable(Variables.SERVICE_BROKER_NAMES_JOB_IDS);

        Map<String, String> serviceBrokerStillInProgress = new HashMap<>();
        for (var entry : serviceBrokerNamesJobIds.entrySet()) {
            String serviceBrokerName = entry.getKey();
            String jobId = entry.getValue();

            AsyncExecutionState asyncJobStatus = checkAsyncJobStatus(context, serviceBrokerName, jobId);
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

    @Override
    public String getPollingErrorMessage(ProcessContext context) {
        return Messages.ERROR_POLLING_ASYNC_SERVICE_BROKERS;
    }

}
