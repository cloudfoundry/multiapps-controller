package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.cloudfoundry.client.lib.domain.CloudTask;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableCloudTask;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.process.Constants;

@RunWith(Parameterized.class)
public class PollExecuteTaskStatusStepTest extends AsyncStepOperationTest<ExecuteTaskStep> {

    @Mock
    private RecentLogsRetriever recentLogsRetriever;

    private static final int START_TIMEOUT = 900;
    private static final String TASK_NAME = "foo";
    private static final String APPLICATION_NAME = "bar";

    private static final UUID TASK_UUID = UUID.randomUUID();
    private static final CloudApplicationExtended APPLICATION = ImmutableCloudApplicationExtended.builder()
                                                                                                 .name(APPLICATION_NAME)
                                                                                                 .build();

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0)
            {
                CloudTask.State.SUCCEEDED, 100L, AsyncExecutionState.FINISHED,
            },
            // (1)
            {
                CloudTask.State.SUCCEEDED, TimeUnit.SECONDS.toMillis(START_TIMEOUT) + 1, AsyncExecutionState.FINISHED,
            },
            // (2)
            {
                CloudTask.State.FAILED, 100L, AsyncExecutionState.ERROR,
            },
            // (3)
            {
                CloudTask.State.FAILED, TimeUnit.SECONDS.toMillis(START_TIMEOUT) + 1, AsyncExecutionState.ERROR,
            },
            // (4)
            {
                CloudTask.State.PENDING, 100L, AsyncExecutionState.RUNNING,
            },
            // (5)
            {
                CloudTask.State.PENDING, TimeUnit.SECONDS.toMillis(START_TIMEOUT) + 1, AsyncExecutionState.RUNNING,
            },
            // (6)
            {
                CloudTask.State.RUNNING, 100L, AsyncExecutionState.RUNNING,
            },
            // (7)
            {
                CloudTask.State.RUNNING, TimeUnit.SECONDS.toMillis(START_TIMEOUT) + 1, AsyncExecutionState.RUNNING,
            },
            // (8)
            {
                CloudTask.State.CANCELING, 100L, AsyncExecutionState.RUNNING,
            },
            // (9)
            {
                CloudTask.State.CANCELING, TimeUnit.SECONDS.toMillis(START_TIMEOUT) + 1, AsyncExecutionState.RUNNING,
            },
// @formatter:on
        });
    }

    private CloudTask.State currentTaskState;
    private long currentTime;
    private AsyncExecutionState expectedExecutionStatus;

    private CloudTask task = ImmutableCloudTask.builder()
                                               .metadata(ImmutableCloudMetadata.builder()
                                                                               .guid(TASK_UUID)
                                                                               .build())
                                               .name(TASK_NAME)
                                               .build();

    public PollExecuteTaskStatusStepTest(CloudTask.State currentTaskState, long currentTime, AsyncExecutionState expectedExecutionStatus) {
        this.currentTaskState = currentTaskState;
        this.currentTime = currentTime;
        this.expectedExecutionStatus = expectedExecutionStatus;
    }

    @Before
    public void setUp() {
        step.currentTimeSupplier = () -> currentTime;
        prepareContext();
        prepareClientExtensions();
    }

    private void prepareContext() {
        StepsUtil.setStartedTask(context, task);
        context.setVariable(Constants.VAR_TASKS_INDEX, 0);
        context.setVariable(Constants.VAR_START_TIME, 0L);
        context.setVariable(Constants.PARAM_START_TIMEOUT, START_TIMEOUT);
        StepsTestUtil.mockApplicationsToDeploy(Arrays.asList(APPLICATION), context);
    }

    private void prepareClientExtensions() {
        CloudTask taskWithState = ImmutableCloudTask.builder()
                                                    .from(task)
                                                    .state(currentTaskState)
                                                    .build();
        when(client.getTask(TASK_UUID)).thenReturn(taskWithState);
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
    protected List<AsyncExecution> getAsyncOperations(ExecutionWrapper wrapper) {
        return step.getAsyncStepExecutions(wrapper);
    }

}
