package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.client.v3.jobs.JobState;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudAsyncJob;

class PollServiceBrokersOperationsExecutionTest extends AsyncStepOperationTest<DeleteServiceBrokersStep> {

    private AsyncExecutionState expectedExecutionStatus;

    static Stream<Arguments> testPollStateExecution() {
        return Stream.of(
                         // (1) All brokers were deleted successfully
                         Arguments.of(List.of(new StepInput("broker1", "1", JobState.COMPLETE),
                                              new StepInput("broker2", "2", JobState.COMPLETE)),
                                      null, AsyncExecutionState.FINISHED),
                         // (2) 1 of 3 brokers were deleted
                         Arguments.of(List.of(new StepInput("broker1", "1", JobState.POLLING),
                                              new StepInput("broker2", "2", JobState.COMPLETE),
                                              new StepInput("broker3", "3", JobState.PROCESSING)),
                                      Map.of("broker1", "1", "broker3", "3"), AsyncExecutionState.RUNNING),
                         // (3) 1 of 3 brokers deletion failed
                         Arguments.of(List.of(new StepInput("broker1", "1", JobState.FAILED),
                                              new StepInput("broker2", "2", JobState.COMPLETE),
                                              new StepInput("broker3", "3", JobState.PROCESSING)),
                                      null, AsyncExecutionState.ERROR));
    }

    @ParameterizedTest
    @MethodSource
    void testPollStateExecution(List<StepInput> stepInput, Map<String, String> expectedServiceBrokerNamesJobIds,
                                AsyncExecutionState expectedExecutionStatus) {
        this.expectedExecutionStatus = expectedExecutionStatus;
        initializeParameters(stepInput);
        testExecuteOperations();

        if (expectedServiceBrokerNamesJobIds != null) {
            assertEquals(expectedServiceBrokerNamesJobIds, context.getVariable(Variables.SERVICE_BROKER_NAMES_JOB_IDS));
        }
    }

    private void initializeParameters(List<StepInput> stepInput) {
        Map<String, String> serviceBrokerNamesJobIds = stepInput.stream()
                                                                .collect(Collectors.toMap(serviceBroker -> serviceBroker.name,
                                                                                          serviceBroker -> serviceBroker.jobId));
        context.setVariable(Variables.SERVICE_BROKER_NAMES_JOB_IDS, serviceBrokerNamesJobIds);

        stepInput.forEach(serviceBroker -> when(client.getAsyncJob(serviceBroker.jobId)).thenReturn(ImmutableCloudAsyncJob.builder()
                                                                                                                          .state(serviceBroker.jobState)
                                                                                                                          .build()));
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
    protected DeleteServiceBrokersStep createStep() {
        return new DeleteServiceBrokersStep();
    }

    private static class StepInput {
        String name;
        String jobId;
        JobState jobState;

        StepInput(String name, String jobId, JobState jobState) {
            this.name = name;
            this.jobId = jobId;
            this.jobState = jobState;
        }

    }

}
