package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;

import org.cloudfoundry.client.v3.jobs.JobState;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sap.cloudfoundry.client.facade.domain.CloudAsyncJob;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudAsyncJob;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceBroker;

class PollServiceBrokerOperationsExecutionTest extends AsyncStepOperationTest<CreateOrUpdateServiceBrokerStep> {

    private static final String JOB_ID = "100";
    private static final String SERVICE_BROKER_NAME = "test-broker";

    private AsyncExecutionState expectedExecutionStatus;

    static Stream<Arguments> testPollStateExecution() {
        return Stream.of(Arguments.of(ImmutableCloudAsyncJob.builder()
                                                            .state(JobState.COMPLETE)
                                                            .build(),
                                      AsyncExecutionState.FINISHED),
                         Arguments.of(ImmutableCloudAsyncJob.builder()
                                                            .state(JobState.FAILED)
                                                            .build(),
                                      AsyncExecutionState.ERROR),
                         Arguments.of(ImmutableCloudAsyncJob.builder()
                                                            .state(JobState.POLLING)
                                                            .build(),
                                      AsyncExecutionState.RUNNING),
                         Arguments.of(ImmutableCloudAsyncJob.builder()
                                                            .state(JobState.PROCESSING)
                                                            .build(),
                                      AsyncExecutionState.RUNNING));
    }

    @ParameterizedTest
    @MethodSource
    void testPollStateExecution(CloudAsyncJob job, AsyncExecutionState expectedExecutionStatus) {
        this.expectedExecutionStatus = expectedExecutionStatus;
        initializeParameters(job);
        testExecuteOperations();
    }

    private void initializeParameters(CloudAsyncJob job) {
        context.setVariable(Variables.CREATED_OR_UPDATED_SERVICE_BROKER, ImmutableCloudServiceBroker.builder()
                                                                                                    .name(SERVICE_BROKER_NAME)
                                                                                                    .build());
        context.setVariable(Variables.SERVICE_BROKER_ASYNC_JOB_ID, JOB_ID);
        when(client.getAsyncJob(JOB_ID)).thenReturn(job);
    }

    @Override
    protected List<AsyncExecution> getAsyncOperations(ProcessContext wrapper) {
        return step.getAsyncStepExecutions(wrapper);
    }

    @Override
    protected void validateOperationExecutionResult(AsyncExecutionState result) {
        assertEquals(expectedExecutionStatus, result);
    }

    @Override
    protected CreateOrUpdateServiceBrokerStep createStep() {
        return new CreateOrUpdateServiceBrokerStep();
    }

}
