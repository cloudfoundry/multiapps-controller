package org.cloudfoundry.multiapps.controller.process.util;

import java.text.MessageFormat;
import java.util.function.Consumer;

import org.cloudfoundry.client.v3.jobs.JobState;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.steps.AsyncExecutionState;
import org.immutables.value.Value;

import com.sap.cloudfoundry.client.facade.domain.CloudAsyncJob;

@Value.Immutable
public interface AsyncJobToAsyncExecutionStateAdapter {

    Consumer<CloudAsyncJob> getOnErrorHandler();

    Consumer<CloudAsyncJob> getOnErrorHandlerForOptionalResource();

    Consumer<CloudAsyncJob> getOnCompleteHandler();

    Consumer<CloudAsyncJob> getInProgressHandler();

    @Value.Default
    default boolean isOptionalResource() {
        return false;
    }

    default AsyncExecutionState evaluateState(CloudAsyncJob asyncJob) {
        if (asyncJob.getState() == JobState.FAILED) {
            return mapFailedAsyncJobState(asyncJob);
        }
        if (asyncJob.getState() == JobState.COMPLETE) {
            getOnCompleteHandler().accept(asyncJob);
            return AsyncExecutionState.FINISHED;
        }
        if (asyncJob.getState() == JobState.PROCESSING || asyncJob.getState() == JobState.POLLING) {
            getInProgressHandler().accept(asyncJob);
            return AsyncExecutionState.RUNNING;
        }
        throw new IllegalStateException(MessageFormat.format(Messages.INVALID_JOB_STATE_PROVIDED_0, asyncJob.getState()));
    }

    private AsyncExecutionState mapFailedAsyncJobState(CloudAsyncJob asyncJob) {
        if (isOptionalResource()) {
            getOnErrorHandlerForOptionalResource().accept(asyncJob);
            return AsyncExecutionState.FINISHED;
        }
        getOnErrorHandler().accept(asyncJob);
        return AsyncExecutionState.ERROR;
    }

}
