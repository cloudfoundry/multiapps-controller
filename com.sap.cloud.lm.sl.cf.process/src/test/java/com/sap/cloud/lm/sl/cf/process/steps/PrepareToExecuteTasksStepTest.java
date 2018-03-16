package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudInfo;
import org.junit.Test;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudInfoExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudTask;
import com.sap.cloud.lm.sl.cf.process.Constants;

public class PrepareToExecuteTasksStepTest extends SyncActivitiStepTest<PrepareToExecuteTasksStep> {

    @Override
    protected PrepareToExecuteTasksStep createStep() {
        return new PrepareToExecuteTasksStep();
    }

    @Test
    public void testIterationOverTasksIsInitialized() throws Exception {
        // Given:
        StepsTestUtil.mockApplicationsToDeploy(Arrays.asList(createDummyApplicationWithTasks(3)), context);

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
    public void testExecuteWhenClientSupportsTasks() throws Exception {
        // Given:
        StepsTestUtil.mockApplicationsToDeploy(Arrays.asList(createDummyApplicationWithTasks(0)), context);

        CloudFoundryOperations client = getClientThatSupportsTasks();
        when(clientProvider.getCloudFoundryClient(anyString(), anyString(), anyString(), anyString())).thenReturn(client);

        // When:
        step.execute(context);

        // Then:
        assertFalse((boolean) context.getVariable(Constants.VAR_PLATFORM_SUPPORTS_TASKS));
    }

    @Test
    public void testExecuteWhenControllerSupportsTasks() throws Exception {
        // Given:
        StepsTestUtil.mockApplicationsToDeploy(Arrays.asList(createDummyApplicationWithTasks(0)), context);

        CloudFoundryOperations client = Mockito.mock(CloudFoundryOperations.class);
        mockControllerTasksSupport(client);
        when(clientProvider.getCloudFoundryClient(anyString(), anyString(), anyString(), anyString())).thenReturn(client);

        // When:
        step.execute(context);

        // Then:
        assertFalse((boolean) context.getVariable(Constants.VAR_PLATFORM_SUPPORTS_TASKS));
    }

    @Test
    public void testExecuteWhenBothClientAndControllerSupportTasks() throws Exception {
        // Given:
        StepsTestUtil.mockApplicationsToDeploy(Arrays.asList(createDummyApplicationWithTasks(0)), context);

        CloudFoundryOperations client = getClientThatSupportsTasks();
        mockControllerTasksSupport(client);
        when(clientProvider.getCloudFoundryClient(anyString(), anyString(), anyString(), anyString())).thenReturn(client);

        // When:
        step.execute(context);

        // Then:
        assertTrue((boolean) context.getVariable(Constants.VAR_PLATFORM_SUPPORTS_TASKS));
    }

    private void mockControllerTasksSupport(CloudFoundryOperations client) {
        CloudInfo info = getInfoSayingTheControllerSupportsTasks();
        when(client.getCloudInfo()).thenReturn(info);
    }

    private CloudInfo getInfoSayingTheControllerSupportsTasks() {
        CloudInfoExtended info = Mockito.mock(CloudInfoExtended.class);
        when(info.hasTasksSupport()).thenReturn(true);
        return info;
    }

    private CloudFoundryOperations getClientThatSupportsTasks() {
        return Mockito.mock(CloudFoundryOperations.class, withSettings().extraInterfaces(ClientExtensions.class));
    }

    private CloudApplicationExtended createDummyApplicationWithTasks(int numberOfTasks) {
        CloudApplicationExtended app = new CloudApplicationExtended(null, "dummy");
        app.setTasks(createDummyTasks(numberOfTasks));
        return app;
    }

    private List<CloudTask> createDummyTasks(int count) {
        List<CloudTask> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(createDummyTask());
        }
        return result;
    }

    private CloudTask createDummyTask() {
        return new CloudTask(null, "dummy");
    }

}
