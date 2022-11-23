package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.cloudfoundry.client.v3.jobs.JobState;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;

import com.sap.cloudfoundry.client.facade.domain.CloudAsyncJob;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudAsyncJob;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;

class PollServiceUnbindingOperationExecutionTest extends AsyncStepOperationTest<UnbindServiceFromApplicationStep> {

    private static final String JOB_ID = "123";
    private static final UUID SERVICE_BINDING_GUID = UUID.randomUUID();

    private AsyncExecutionState expectedAsyncExecutionState;

    @Test
    void testExecution() {
        context.setVariable(Variables.SERVICE_UNBINDING_JOB_ID, JOB_ID);
        CloudAsyncJob asyncJob = buildAsyncJob();
        when(client.getAsyncJob(JOB_ID)).thenReturn(asyncJob);
        expectedAsyncExecutionState = AsyncExecutionState.FINISHED;
        testExecuteOperations();
    }

    private CloudAsyncJob buildAsyncJob() {
        return ImmutableCloudAsyncJob.builder()
                                     .state(JobState.COMPLETE)
                                     .metadata(ImmutableCloudMetadata.of(SERVICE_BINDING_GUID))
                                     .build();
    }

    @Override
    protected List<AsyncExecution> getAsyncOperations(ProcessContext wrapper) {
        return List.of(new PollServiceUnbindingOperationExecution());
    }

    @Override
    protected void validateOperationExecutionResult(AsyncExecutionState result) {
        assertEquals(expectedAsyncExecutionState, result);
    }

    @Override
    protected UnbindServiceFromApplicationStep createStep() {
        return new UnbindServiceFromApplicationStep();
    }
}
