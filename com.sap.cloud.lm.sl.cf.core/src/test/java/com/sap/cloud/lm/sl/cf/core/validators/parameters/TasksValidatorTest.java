package com.sap.cloud.lm.sl.cf.core.validators.parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.mta.model.v2.Module;

@RunWith(Parameterized.class)
public class TasksValidatorTest {

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Valid tasks:
            {
                "tasks-00.json", true,
            },
            // (1) The tasks are an empty list:
            {
                "tasks-01.json", true,
            },
            // (2) The tasks are not a list of maps, but a list of strings:
            {
                "tasks-02.json", false,
            },
            // (3) The tasks are not a list but a map:
            {
                "tasks-03.json", false,
            },
            // (4) The name of a task is not a string:
            {
                "tasks-04.json", false,
            },
            // (5) The command of a task is not a string:
            {
                "tasks-05.json", false,
            },
            // (6) The name of a task is not specified:
            {
                "tasks-06.json", false,
            },
            // (7) The command of a task is not specified:
            {
                "tasks-07.json", false,
            },
            // (8) The env of a task is not a map:
            {
                "tasks-08.json", false,
            },
// @formatter:on
        });
    }

    private String locationOfFileContainingTasks;
    private boolean expectedResult;

    public TasksValidatorTest(String locationOfFileContainingTasks, boolean expectedResult) {
        this.locationOfFileContainingTasks = locationOfFileContainingTasks;
        this.expectedResult = expectedResult;
    }

    @Test
    public void testValidate() throws Exception {
        String tasksJson = TestUtil.getResourceAsString(locationOfFileContainingTasks, getClass());
        Object tasks = JsonUtil.fromJson(tasksJson, Object.class);
        assertEquals(expectedResult, new TasksValidator().isValid(tasks));
    }

    @Test
    public void testGetContainerType() {
        assertTrue(new TasksValidator().getContainerType()
            .isAssignableFrom(Module.class));
    }

}
