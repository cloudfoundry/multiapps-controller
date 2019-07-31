package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudTask;
import org.cloudfoundry.client.lib.domain.ImmutableCloudTask;
import org.junit.Test;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.process.Constants;

public class PrepareToExecuteTasksStepTest extends SyncFlowableStepTest<PrepareToExecuteTasksStep> {

    @Override
    protected PrepareToExecuteTasksStep createStep() {
        return new PrepareToExecuteTasksStep();
    }

    @Test
    public void testIterationOverTasksIsInitialized() throws Exception {
        // Given:
        StepsUtil.setTasksToExecute(context, createDummyApplicationWithTasks(3).getTasks());

        // When:
        step.execute(context);

        // Then:
        assertStepFinishedSuccessfully();
        assertEquals(3, context.getVariable(Constants.VAR_TASKS_COUNT));
        assertEquals(0, context.getVariable(Constants.VAR_TASKS_INDEX));
        assertEquals(Constants.VAR_TASKS_INDEX, context.getVariable(Constants.VAR_INDEX_VARIABLE_NAME));
        assertEquals(3, StepsUtil.getTasksToExecute(context)
                                 .size());
    }

    @Test
    public void testExecuteWhenTasksAreSupported() throws Exception {
        // Given:
        StepsUtil.setTasksToExecute(context, createDummyApplicationWithTasks(0).getTasks());
        when(client.areTasksSupported()).thenReturn(true);

        // When:
        step.execute(context);

        // Then:
        assertTrue((boolean) context.getVariable(Constants.VAR_PLATFORM_SUPPORTS_TASKS));
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
