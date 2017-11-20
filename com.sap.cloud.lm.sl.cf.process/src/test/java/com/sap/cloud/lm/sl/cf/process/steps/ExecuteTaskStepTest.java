package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudTask;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.MapUtil;

public class ExecuteTaskStepTest extends SyncActivitiStepTest<ExecuteTaskStep> {

    private static final long DUMMY_TIME = 100;

    private final CloudTask task = new CloudTask(null, "foo");
    private final CloudApplicationExtended app = new CloudApplicationExtended(null, "dummy");

    @Before
    public void setUp() {
        step.currentTimeSupplier = () -> DUMMY_TIME;
        task.setCommand("echo ${test}");
        task.setEnvironmentVariables(MapUtil.asMap("test", "bar"));
    }

    @Test
    public void testExecute() throws Exception {
        // Given:
        StepsTestUtil.mockApplicationsToDeploy(Arrays.asList(app), context);
        StepsUtil.setTasksToExecute(context, Arrays.asList(task));
        context.setVariable(Constants.VAR_TASKS_INDEX, 0);

        when(clientExtensions.runTask(app.getName(), task.getName(), task.getCommand(), task.getEnvironmentVariables())).thenReturn(
            StepsTestUtil.copy(task));

        // When:
        step.execute(context);

        // Then:
        assertStepFinishedSuccessfully();
        verifyTaskWasStarted();
    }

    @Override
    protected void assertStepFinishedSuccessfully() {
        assertEquals(ExecutionStatus.RUNNING.toString(), getExecutionStatus());
    }

    private void verifyTaskWasStarted() {
        verify(clientExtensions).runTask(app.getName(), task.getName(), task.getCommand(), task.getEnvironmentVariables());
        assertEquals(DUMMY_TIME, context.getVariable(Constants.VAR_START_TIME));
        String expectedStartedTaskJson = JsonUtil.toJson(task, true);
        String actualStartedTaskJson = JsonUtil.toJson(StepsUtil.getStartedTask(context), true);
        assertEquals(expectedStartedTaskJson, actualStartedTaskJson);
    }

    @Override
    protected ExecuteTaskStep createStep() {
        return new ExecuteTaskStep();
    }

}
