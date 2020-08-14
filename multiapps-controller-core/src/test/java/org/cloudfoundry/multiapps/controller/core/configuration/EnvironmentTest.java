package org.cloudfoundry.multiapps.controller.core.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class EnvironmentTest {

    @Mock
    private EnvironmentVariablesAccessor environmentVariablesAccessor;
    @InjectMocks
    private Environment environment;

    @BeforeEach
    void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testGetAllVariables() {
        Map<String, String> variables = new HashMap<>();
        variables.put("foo", "bar");
        variables.put("baz", "qux");
        Mockito.when(environmentVariablesAccessor.getAllVariables())
               .thenReturn(variables);

        assertEquals(variables, environment.getAllVariables());
    }

    @Test
    void testGetString() {
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn("cd");

        assertEquals("cd", environment.getString("ab"));
    }

    @Test
    void testGetStringWhenVariableIsMissing() {
        assertNull(environment.getString("ab"));
    }

    @Test
    void testGetStringWithDefault() {
        assertEquals("cd", environment.getString("ab", "cd"));
    }

    @Test
    void testGetLong() {
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn("12");

        assertEquals(12, (long) environment.getLong("ab"));
    }

    @Test
    void testGetLongWhenVariableIsMissing() {
        assertNull(environment.getLong("ab"));
    }

    @Test
    void testGetLongWhenVariableIsInvalid() {
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn("cd");

        assertNull(environment.getLong("ab"));
    }

    @Test
    void testGetLongWhenVariableIsInvalidWithDefault() {
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn("cd");

        assertEquals(12, (long) environment.getLong("ab", 12L));
    }

    @Test
    void testGetLongWithDefault() {
        assertEquals(12, (long) environment.getLong("ab", 12L));
    }

    @Test
    void testGetInteger() {
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn("12");

        assertEquals(12, (long) environment.getInteger("ab"));
    }

    @Test
    void testGetIntegerWhenVariableIsMissing() {
        assertNull(environment.getInteger("ab"));
    }

    @Test
    void testGetIntegerWhenVariableIsInvalid() {
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn("cd");

        assertNull(environment.getInteger("ab"));
    }

    @Test
    void testGetIntegerWhenVariableIsInvalidWithDefault() {
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn("cd");

        assertEquals(12, (int) environment.getInteger("ab", 12));
    }

    @Test
    void testGetIntegerWithDefault() {
        assertEquals(12, (int) environment.getInteger("ab", 12));
    }

    @Test
    void testGetBoolean() {
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn("false");

        assertEquals(false, environment.getBoolean("ab"));
    }

    @Test
    void testGetBooleanWhenVariableIsMissing() {
        assertNull(environment.getBoolean("ab"));
    }

    @Test
    void testGetBooleanWhenVariableIsInvalid() {
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn("cd");

        assertEquals(false, environment.getBoolean("ab"));
    }

    @Test
    void testGetBooleanWithDefault() {
        assertEquals(false, environment.getBoolean("ab", false));
    }

    @Test
    void testGetPositiveIntegerWithNegativeVariable() {
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn("-1");

        assertEquals(Integer.MAX_VALUE, (int) environment.getPositiveInteger("ab", null));
    }

    @Test
    void testGetPositiveIntegerWithPositiveVariable() {
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn("+1");

        assertEquals(+1, (int) environment.getPositiveInteger("ab", null));
    }

    @Test
    void testGetPositiveIntegerWithZero() {
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn("0");

        assertEquals(Integer.MAX_VALUE, (int) environment.getPositiveInteger("ab", null));
    }

    @Test
    void testGetPositiveIntegerWithNull() {
        assertEquals(Integer.MAX_VALUE, (int) environment.getPositiveInteger("ab", null));
    }

    @Test
    void testGetNegativeIntegerWithNegativeVariable() {
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn("-1");

        assertEquals(-1, (int) environment.getNegativeInteger("ab", null));
    }

    @Test
    void testGetNegativeIntegerWithPositiveVariable() {
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn("+1");

        assertEquals(Integer.MIN_VALUE, (int) environment.getNegativeInteger("ab", null));
    }

    @Test
    void testGetNegativeIntegerWithZero() {
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn("0");

        assertEquals(Integer.MIN_VALUE, (int) environment.getNegativeInteger("ab", null));
    }

    @Test
    void testGetNegativeIntegerWithNull() {
        assertEquals(Integer.MIN_VALUE, (int) environment.getNegativeInteger("ab", null));
    }

    @Test
    void tetGetVariable() {
        UUID expectedUuid = UUID.fromString("c9fbfbfd-8d54-4cba-958f-25a3f7a14ca7");
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn(expectedUuid.toString());

        assertEquals(expectedUuid, environment.getVariable("ab", UUID::fromString));
    }

    @Test
    void testHasVariable() {
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn("0");

        assertTrue(environment.hasVariable("ab"));
    }

    @Test
    void testHasVariableWhenVariableIsAnEmptyString() {
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn("");

        assertFalse(environment.hasVariable("ab"));
    }

    @Test
    void testHasVariableWhenVariableIsMissing() {
        assertFalse(environment.hasVariable("ab"));
    }

}
