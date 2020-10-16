package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.cloudfoundry.multiapps.common.test.GenericArgumentMatcher;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cloudfoundry.client.facade.domain.CloudTask;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudTask;

class ExecuteTaskStepTest extends SyncFlowableStepTest<ExecuteTaskStep> {

    private static final Long DUMMY_TIME = 100L;

    private final CloudTask task = ImmutableCloudTask.builder()
                                                     .name("foo")
                                                     .command("echo ${test}")
                                                     .build();
    private final CloudApplicationExtended app = ImmutableCloudApplicationExtended.builder()
                                                                                  .name("dummy")
                                                                                  .build();

    @BeforeEach
    public void setUp() {
        step.currentTimeSupplier = () -> DUMMY_TIME;
    }

    @Test
    void testExecute() {
        // Given:
        StepsTestUtil.mockApplicationsToDeploy(List.of(app), execution);
        context.setVariable(Variables.TASKS_TO_EXECUTE, List.of(task));
        context.setVariable(Variables.TASKS_INDEX, 0);

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
        assertEquals(DUMMY_TIME, context.getVariable(Variables.START_TIME));
        CloudTask startedTask = context.getVariable(Variables.STARTED_TASK);
        assertEquals(task.getName(), startedTask.getName());
        assertEquals(task.getCommand(), startedTask.getCommand());
    }

    @Override
    protected ExecuteTaskStep createStep() {
        return new ExecuteTaskStep();
    }

}
