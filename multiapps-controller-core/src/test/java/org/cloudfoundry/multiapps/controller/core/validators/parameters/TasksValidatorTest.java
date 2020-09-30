package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TasksValidatorTest {

    public static Stream<Arguments> testValidate() {
        return Stream.of(
// @formatter:off
            // (0) Valid tasks:
            Arguments.of("tasks-00.json", true),
            // (1) The tasks are an empty list:
            Arguments.of("tasks-01.json", true),
            // (2) The tasks are not a list of maps, but a list of strings:
            Arguments.of("tasks-02.json", false),
            // (3) The tasks are not a list but a map:
            Arguments.of("tasks-03.json", false),
            // (4) The name of a task is not a string:
            Arguments.of("tasks-04.json", false),
            // (5) The command of a task is not a string:
            Arguments.of("tasks-05.json", false),
            // (6) The name of a task is not specified:
            Arguments.of("tasks-06.json", false),
            // (7) The command of a task is not specified:
            Arguments.of("tasks-07.json", false)
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testValidate(String locationOfFileContainingTasks, boolean expectedResult) {
        String tasksJson = TestUtil.getResourceAsString(locationOfFileContainingTasks, getClass());
        Object tasks = JsonUtil.fromJson(tasksJson, Object.class);
        assertEquals(expectedResult, new TasksValidator().isValid(tasks, null));
    }

    @Test
    void testGetContainerType() {
        assertTrue(new TasksValidator().getContainerType()
                                       .isAssignableFrom(Module.class));
    }

}
