package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientFactory;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sap.cloudfoundry.client.facade.adapters.LogCacheClient;
import com.sap.cloudfoundry.client.facade.domain.CloudTask;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudTask;
import org.mockito.Mock;
import org.mockito.Mockito;

class PollExecuteTaskStatusStepTest extends AsyncStepOperationTest<ExecuteTaskStep> {

    private static final Duration START_TIMEOUT = Duration.ofSeconds(900);
    private static final String TASK_NAME = "foo";
    private static final String APPLICATION_NAME = "bar";

    private static final UUID TASK_UUID = UUID.randomUUID();
    private static final CloudApplicationExtended APPLICATION = ImmutableCloudApplicationExtended.builder()
                                                                                                 .name(APPLICATION_NAME)
                                                                                                 .build();

    private final CloudTask task = ImmutableCloudTask.builder()
                                                     .metadata(ImmutableCloudMetadata.builder()
                                                                                     .guid(TASK_UUID)
                                                                                     .build())
                                                     .name(TASK_NAME)
                                                     .build();

    private AsyncExecutionState expectedExecutionStatus;

    @Mock
    private CloudControllerClientFactory clientFactory;
    @Mock
    private TokenService tokenService;

    public static Stream<Arguments> testPollStateExecution() {
        return Stream.of(
// @formatter:off
            // (0)
            Arguments.of(CloudTask.State.SUCCEEDED, 100L, AsyncExecutionState.FINISHED),
            // (1)
            Arguments.of(CloudTask.State.SUCCEEDED, START_TIMEOUT.toMillis() + 1, AsyncExecutionState.FINISHED),
            // (2)
            Arguments.of(CloudTask.State.FAILED, 100L, AsyncExecutionState.ERROR),
            // (3)
            Arguments.of(CloudTask.State.FAILED, START_TIMEOUT.toMillis() + 1, AsyncExecutionState.ERROR),
            // (4)
            Arguments.of(CloudTask.State.PENDING, 100L, AsyncExecutionState.RUNNING),
            // (5)
            Arguments.of(CloudTask.State.PENDING, START_TIMEOUT.toMillis() + 1, AsyncExecutionState.RUNNING),
            // (6)
            Arguments.of(CloudTask.State.RUNNING, 100L, AsyncExecutionState.RUNNING),
            // (7)
            Arguments.of(CloudTask.State.RUNNING, START_TIMEOUT.toMillis() + 1, AsyncExecutionState.RUNNING),
            // (8)
            Arguments.of(CloudTask.State.CANCELING, 100L, AsyncExecutionState.RUNNING),
            // (9)
            Arguments.of(CloudTask.State.CANCELING, START_TIMEOUT.toMillis() + 1, AsyncExecutionState.RUNNING)
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testPollStateExecution(CloudTask.State currentTaskState, long currentTime, AsyncExecutionState expectedExecutionStatus) {
        this.expectedExecutionStatus = expectedExecutionStatus;
        initializeParameters(currentTaskState, currentTime);
        testExecuteOperations();
    }

    private void initializeParameters(CloudTask.State currentTaskState, long currentTime) {
        step.currentTimeSupplier = () -> currentTime;
        prepareContext();
        prepareClientExtensions(currentTaskState);
    }

    private void prepareContext() {
        context.setVariable(Variables.STARTED_TASK, task);
        context.setVariable(Variables.TASKS_INDEX, 0);
        context.setVariable(Variables.START_TIME, 0L);
        context.setVariable(Variables.START_TIMEOUT, START_TIMEOUT);
        StepsTestUtil.mockApplicationsToDeploy(List.of(APPLICATION), execution);
    }

    private void prepareClientExtensions(CloudTask.State currentTaskState) {
        CloudTask taskWithState = ImmutableCloudTask.builder()
                                                    .from(task)
                                                    .state(currentTaskState)
                                                    .build();
        when(client.getTask(TASK_UUID)).thenReturn(taskWithState);

        var logCacheClient = Mockito.mock(LogCacheClient.class);
        when(logCacheClient.getRecentLogs(any(UUID.class), any())).thenReturn(Collections.emptyList());
        when(clientFactory.createLogCacheClient(any(), any())).thenReturn(logCacheClient);
    }

    @Override
    protected void validateOperationExecutionResult(AsyncExecutionState result) {
        assertEquals(expectedExecutionStatus.toString(), result.toString());
    }

    @Override
    protected ExecuteTaskStep createStep() {
        return new ExecuteTaskStep();
    }

    @Override
    protected List<AsyncExecution> getAsyncOperations(ProcessContext wrapper) {
        return step.getAsyncStepExecutions(wrapper);
    }

}
