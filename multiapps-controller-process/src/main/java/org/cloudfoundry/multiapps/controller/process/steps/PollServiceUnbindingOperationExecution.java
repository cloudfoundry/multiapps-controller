package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.multiapps.controller.process.variables.Variables;

public class PollServiceUnbindingOperationExecution extends PollServiceBindingUnbindingOperationBaseExecution {

    @Override
    public AsyncExecutionState execute(ProcessContext context) {
        List<String> jobIds = context.getVariable(Variables.SERVICE_UNBINDING_JOB_IDS);
        if (jobIds.isEmpty()) {
            return super.execute(context);
        }
        List<String> remainingJobIds = new ArrayList<>(jobIds);
        for (String jobId : jobIds) {
            context.setVariable(Variables.SERVICE_UNBINDING_JOB_ID, jobId);
            AsyncExecutionState state = super.execute(context);
            if (state == AsyncExecutionState.FINISHED) {
                remainingJobIds.remove(jobId);
                context.setVariable(Variables.SERVICE_UNBINDING_JOB_IDS, remainingJobIds);
                continue;
            }
            return state;
        }
        return AsyncExecutionState.FINISHED;
    }

    @Override
    protected String getAsyncJobId(ProcessContext context) {
        return context.getVariable(Variables.SERVICE_UNBINDING_JOB_ID);
    }

}
