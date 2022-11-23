package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.function.Consumer;

import org.cloudfoundry.multiapps.controller.process.util.AsyncJobToAsyncExecutionStateAdapter;
import org.cloudfoundry.multiapps.controller.process.util.ImmutableAsyncJobToAsyncExecutionStateAdapter;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudAsyncJob;

public abstract class PollOperationBaseExecution implements AsyncExecution {

    @Override
    public AsyncExecutionState execute(ProcessContext context) {
        String asyncJobId = getAsyncJobId(context);
        CloudControllerClient controllerClient = context.getControllerClient();
        CloudAsyncJob asyncJob = controllerClient.getAsyncJob(asyncJobId);
        AsyncJobToAsyncExecutionStateAdapter asyncJobToAsyncExecutionStateAdapter = buildAsyncJobToAsyncExecutionAdapter(context);
        return asyncJobToAsyncExecutionStateAdapter.evaluateState(asyncJob);
    }

    private AsyncJobToAsyncExecutionStateAdapter buildAsyncJobToAsyncExecutionAdapter(ProcessContext context) {
        return ImmutableAsyncJobToAsyncExecutionStateAdapter.builder()
                                                            .inProgressHandler(getInProgressHandler(context))
                                                            .onCompleteHandler(getOnCompleteHandler(context))
                                                            .onErrorHandler(getOnErrorHandler(context))
                                                            .onErrorHandlerForOptionalResource(getOnErrorHandlerForOptionalResource(context))
                                                            .isOptionalResource(isOptional(context))
                                                            .build();
    }

    protected abstract String getAsyncJobId(ProcessContext context);

    protected abstract boolean isOptional(ProcessContext context);

    protected abstract Consumer<CloudAsyncJob> getInProgressHandler(ProcessContext context);

    protected abstract Consumer<CloudAsyncJob> getOnCompleteHandler(ProcessContext context);

    protected abstract Consumer<CloudAsyncJob> getOnErrorHandler(ProcessContext context);

    protected abstract Consumer<CloudAsyncJob> getOnErrorHandlerForOptionalResource(ProcessContext context);

}
