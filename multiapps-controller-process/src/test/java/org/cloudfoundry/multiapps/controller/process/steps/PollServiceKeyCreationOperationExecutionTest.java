package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.cloudfoundry.client.v3.jobs.JobState;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sap.cloudfoundry.client.facade.domain.CloudAsyncJob;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudAsyncJob;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;

class PollServiceKeyCreationOperationExecutionTest extends AsyncStepOperationTest<CreateServiceKeyStep> {

    private static final String SERVICE_KEY_JOB_ID = "123";
    private static final UUID SERVICE_KEY_GUID = UUID.randomUUID();
    private static final String SERVICE_INSTANCE_NAME = "test_service_instance";
    private static final UUID SERVICE_INSTANCE_GUID = UUID.randomUUID();

    private AsyncExecutionState expectedAsyncExecutionState;

    static Stream<Arguments> testServiceKeyPollingWithDifferentStates() {
        return Stream.of(Arguments.of(JobState.COMPLETE, AsyncExecutionState.FINISHED),
                         Arguments.of(JobState.POLLING, AsyncExecutionState.RUNNING),
                         Arguments.of(JobState.PROCESSING, AsyncExecutionState.RUNNING),
                         Arguments.of(JobState.FAILED, AsyncExecutionState.ERROR));
    }

    @MethodSource
    @ParameterizedTest
    void testServiceKeyPollingWithDifferentStates(JobState serviceKeyOperationJobState, AsyncExecutionState expectedAsyncExecutionState) {
        this.expectedAsyncExecutionState = expectedAsyncExecutionState;
        context.setVariable(Variables.SERVICE_KEY_CREATION_JOB_ID, SERVICE_KEY_JOB_ID);
        CloudAsyncJob asyncJob = buildCloudAsyncJob(serviceKeyOperationJobState);
        context.setVariable(Variables.SERVICE_TO_PROCESS, buildOptionalCloudServiceInstance(false));
        when(client.getAsyncJob(SERVICE_KEY_JOB_ID)).thenReturn(asyncJob);
        testExecuteOperations();
    }

    @Test
    void testServiceKeyCreationFailureWithOptionalService() {
        expectedAsyncExecutionState = AsyncExecutionState.FINISHED;
        context.setVariable(Variables.SERVICE_KEY_CREATION_JOB_ID, SERVICE_KEY_JOB_ID);
        CloudServiceInstanceExtended optionalServiceInstance = buildOptionalCloudServiceInstance(true);
        context.setVariable(Variables.SERVICE_TO_PROCESS, optionalServiceInstance);
        CloudAsyncJob asyncJob = buildCloudAsyncJob(JobState.FAILED);
        when(client.getAsyncJob(SERVICE_KEY_JOB_ID)).thenReturn(asyncJob);
        testExecuteOperations();
    }

    private CloudAsyncJob buildCloudAsyncJob(JobState asyncJobState) {
        return ImmutableCloudAsyncJob.builder()
                                     .state(asyncJobState)
                                     .operation("CREATE")
                                     .metadata(ImmutableCloudMetadata.of(SERVICE_KEY_GUID))
                                     .build();
    }

    private CloudServiceInstanceExtended buildOptionalCloudServiceInstance(boolean isOptional) {
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .name(SERVICE_INSTANCE_NAME)
                                                    .metadata(ImmutableCloudMetadata.of(SERVICE_INSTANCE_GUID))
                                                    .isOptional(isOptional)
                                                    .build();
    }

    @Override
    protected List<AsyncExecution> getAsyncOperations(ProcessContext wrapper) {
        return List.of(new PollServiceKeyCreationOperationExecution());
    }

    @Override
    protected void validateOperationExecutionResult(AsyncExecutionState result) {
        assertEquals(expectedAsyncExecutionState, result);
    }

    @Override
    protected CreateServiceKeyStep createStep() {
        return new CreateServiceKeyStep();
    }
}
