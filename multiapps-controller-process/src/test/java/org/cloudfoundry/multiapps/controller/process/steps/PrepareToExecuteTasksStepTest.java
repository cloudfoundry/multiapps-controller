package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudTask;
import org.cloudfoundry.client.lib.domain.ImmutableCloudTask;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;

class PrepareToExecuteTasksStepTest extends SyncFlowableStepTest<PrepareToExecuteTasksStep> {

    @Override
    protected PrepareToExecuteTasksStep createStep() {
        return new PrepareToExecuteTasksStep();
    }

    @Test
    void testIterationOverTasksIsInitialized() {
        // Given:
        context.setVariable(Variables.TASKS_TO_EXECUTE, createDummyApplicationWithTasks(3).getTasks());

        // When:
        step.execute(execution);

        // Then:
        assertStepFinishedSuccessfully();
        assertEquals((Integer) 3, context.getVariable(Variables.TASKS_COUNT));
        assertEquals((Integer) 0, context.getVariable(Variables.TASKS_INDEX));
        assertEquals(Variables.TASKS_INDEX.getName(), context.getVariable(Variables.INDEX_VARIABLE_NAME));
        assertEquals(3, context.getVariable(Variables.TASKS_TO_EXECUTE)
                               .size());
    }

    @Test
    void testExecuteWhenTasksAreSupported() {
        // Given:
        context.setVariable(Variables.TASKS_TO_EXECUTE, createDummyApplicationWithTasks(0).getTasks());

        // When:
        step.execute(execution);

        assertStepFinishedSuccessfully();
    }

    private CloudApplicationExtended createDummyApplicationWithTasks(int numberOfTasks) {
        return ImmutableCloudApplicationExtended.builder()
                                                .name("dummy")
                                                .tasks(createDummyTasks(numberOfTasks))
                                                .build();
    }

    private List<CloudTask> createDummyTasks(int count) {
        List<CloudTask> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(createDummyTask());
        }
        return result;
    }

    private CloudTask createDummyTask() {
        return ImmutableCloudTask.builder()
                                 .name("dummy")
                                 .build();
    }

}
