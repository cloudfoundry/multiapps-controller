package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.cloudfoundry.client.lib.domain.CloudEntity.Meta;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudTask;
import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;
import com.sap.cloud.lm.sl.cf.process.Constants;

@RunWith(Parameterized.class)
public class PollExecuteTaskStatusStepTest extends AbstractStepTest<PollExecuteTaskStatusStep> {

    @Mock
    private RecentLogsRetriever recentLogsRetriever;

    private static final int START_TIMEOUT = 900;
    private static final String TASK_NAME = "foo";
    private static final String APPLICATION_NAME = "bar";

    private static final UUID TASK_UUID = UUID.randomUUID();
    private static final CloudApplicationExtended APPLICATION = new CloudApplicationExtended(null, APPLICATION_NAME);

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0)
            {
                CloudTask.State.SUCCEEDED, 100L, ExecutionStatus.SUCCESS,
            },
            // (1)
            {
                CloudTask.State.SUCCEEDED, TimeUnit.SECONDS.toMillis(START_TIMEOUT) + 1, ExecutionStatus.SUCCESS,
            },
            // (2)
            {
                CloudTask.State.FAILED, 100L, ExecutionStatus.LOGICAL_RETRY,
            },
            // (3)
            {
                CloudTask.State.FAILED, TimeUnit.SECONDS.toMillis(START_TIMEOUT) + 1, ExecutionStatus.LOGICAL_RETRY,
            },
            // (4)
            {
                CloudTask.State.PENDING, 100L, ExecutionStatus.RUNNING,
            },
            // (5)
            {
                CloudTask.State.PENDING, TimeUnit.SECONDS.toMillis(START_TIMEOUT) + 1, ExecutionStatus.LOGICAL_RETRY,
            },
            // (6)
            {
                CloudTask.State.RUNNING, 100L, ExecutionStatus.RUNNING,
            },
            // (7)
            {
                CloudTask.State.RUNNING, TimeUnit.SECONDS.toMillis(START_TIMEOUT) + 1, ExecutionStatus.LOGICAL_RETRY,
            },
            // (8)
            {
                CloudTask.State.CANCELING, 100L, ExecutionStatus.RUNNING,
            },
            // (9)
            {
                CloudTask.State.CANCELING, TimeUnit.SECONDS.toMillis(START_TIMEOUT) + 1, ExecutionStatus.LOGICAL_RETRY,
            },
// @formatter:on
        });
    }

    private CloudTask.State currentTaskState;
    private long currentTime;
    private ExecutionStatus expectedExecutionStatus;

    private CloudTask task = new CloudTask(new Meta(TASK_UUID, null, null), TASK_NAME);

    public PollExecuteTaskStatusStepTest(CloudTask.State currentTaskState, long currentTime, ExecutionStatus expectedExecutionStatus) {
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
        CloudTask taskWithState = StepsTestUtil.copy(task);
        taskWithState.setState(currentTaskState);
        when(clientExtensions.getTasks(APPLICATION_NAME)).thenReturn(Arrays.asList(taskWithState));
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertEquals(expectedExecutionStatus.toString(), getExecutionStatus());
    }

    @Override
    protected PollExecuteTaskStatusStep createStep() {
        return new PollExecuteTaskStatusStep();
    }

}
