package com.sap.cloud.lm.sl.cf.process.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudTask;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.google.gson.reflect.TypeToken;
import com.sap.cloud.lm.sl.cf.core.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.cf.core.model.HookPhase;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.util.HookExecutor.HookExecution;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.mta.model.Hook;

public class TasksHookExecutorTest {

    private static final String DEFAULT_PROCESS_ID = "DEFAULT_PROCESS_ID";
    @Mock
    private DelegateExecution context;
    @Mock
    private FlowableFacade flowableFacade;

    private TasksHookExecutor tasksHookExecutor;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(context.getId())
            .thenReturn(DEFAULT_PROCESS_ID);
        tasksHookExecutor = new TasksHookExecutor(context, flowableFacade);
    }

    @Test
    public void testExecuteHooksWithHookWithNoTasksProvided() {
        Hook testHook = Hook.createV3()
            .setParameters(Collections.emptyMap())
            .setName("foo")
            .setPhases(Arrays.asList("this-is-test-not-important"));
        HookExecution hookExecution = new HookExecution(HookPhase.APPLICATION_AFTER_STOP_LIVE, testHook, "testCompleteMessage");

        Assertions.assertThrows(IllegalStateException.class, () -> tasksHookExecutor.executeHook(hookExecution),
            "Hook task parameters must not be empty");

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testExecuteReqularHookWithOneTask() {
        Hook testHook = Hook.createV3()
            .setParameters(buildTask())
            .setName("foo")
            .setPhases(Arrays.asList("this-is-test-not-important"));

        HookExecution hookExecution = new HookExecution(HookPhase.APPLICATION_AFTER_STOP_LIVE, testHook, "testCompleteMessage");

        tasksHookExecutor.executeHook(hookExecution);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Map> processVariablesCaptor = ArgumentCaptor.forClass(Map.class);

        Mockito.verify(flowableFacade)
            .startProcessByMessage(Mockito.eq("APPLICATION_AFTER_STOP_LIVE"), processVariablesCaptor.capture());
        Map<String, Object> processVariables = processVariablesCaptor.getValue();
        validateProcessVariables(processVariables);
    }

    private void validateProcessVariables(Map<String, Object> processVariables) {
        Assertions.assertEquals("testCompleteMessage", processVariables.get("onCompleteMessageEventName"));
        Assertions.assertEquals(DEFAULT_PROCESS_ID, processVariables.get("parentExecutionId"));
        validateTasks((byte[]) processVariables.get(Constants.VAR_TASKS_TO_EXECUTE));

    }

    private void validateTasks(byte[] cloudTasksBinary) {
        Assertions.assertNotNull(cloudTasksBinary);
        List<CloudTask> cloudTasks = JsonUtil.fromJsonBinary(cloudTasksBinary, new TypeToken<List<CloudTask>>() {
        }.getType());
        Assertions.assertTrue(!cloudTasks.isEmpty());
        Assertions.assertTrue(cloudTasks.size() == 1);
        CloudTask cloudTask = cloudTasks.get(0);
        Assertions.assertEquals("test-task-name", cloudTask.getName());
        Assertions.assertEquals("foo", cloudTask.getCommand());
    }

    private Map<String, Object> buildTask() {
        Map<String, Object> result = new HashMap<>();
        result.put("name", "test-task-name");
        result.put("command", "foo");
        return result;
    }

}
