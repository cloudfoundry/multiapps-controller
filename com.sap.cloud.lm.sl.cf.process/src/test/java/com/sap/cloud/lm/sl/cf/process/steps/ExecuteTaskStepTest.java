package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.cloudfoundry.client.lib.domain.CloudTask;
import org.cloudfoundry.client.lib.domain.ImmutableCloudTask;
import org.junit.Before;
import org.junit.Test;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.common.util.GenericArgumentMatcher;

public class ExecuteTaskStepTest extends SyncFlowableStepTest<ExecuteTaskStep> {

    private static final long DUMMY_TIME = 100;

    private final CloudTask task = ImmutableCloudTask.builder()
                                                     .name("foo")
                                                     .command("echo ${test}")
                                                     .build();
    private final CloudApplicationExtended app = ImmutableCloudApplicationExtended.builder()
                                                                                  .name("dummy")
                                                                                  .build();

    @Before
    public void setUp() {
        step.currentTimeSupplier = () -> DUMMY_TIME;
    }

    @Test
    public void testExecute() {
        // Given:
        StepsTestUtil.mockApplicationsToDeploy(Collections.singletonList(app), execution);
        context.setVariable(Variables.TASKS_TO_EXECUTE, Collections.singletonList(task));
        execution.setVariable(Constants.VAR_TASKS_INDEX, 0);

        when(client.runTask(eq(app.getName()),
                            argThat(GenericArgumentMatcher.forObject(task)))).thenReturn(ImmutableCloudTask.copyOf(task));

        // When:
        step.execute(execution);

        // Then:
        assertStepFinishedSuccessfully();
        verifyTaskWasStarted();
    }

    @Override
    protected void assertStepFinishedSuccessfully() {
        assertEquals(StepPhase.POLL.toString(), getExecutionStatus());
    }

    private void verifyTaskWasStarted() {
        verify(client).runTask(eq(app.getName()), argThat(GenericArgumentMatcher.forObject(task)));
        assertEquals(DUMMY_TIME, execution.getVariable(Constants.VAR_START_TIME));
        CloudTask startedTask = context.getVariable(Variables.STARTED_TASK);
        assertEquals(task.getName(), startedTask.getName());
        assertEquals(task.getCommand(), startedTask.getCommand());
    }

    @Override
    protected ExecuteTaskStep createStep() {
        return new ExecuteTaskStep();
    }

}
