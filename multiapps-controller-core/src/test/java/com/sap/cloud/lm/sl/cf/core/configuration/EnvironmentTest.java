package com.sap.cloud.lm.sl.cf.core.configuration;

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

public class EnvironmentTest {

    @Mock
    private EnvironmentVariablesAccessor environmentVariablesAccessor;
    @InjectMocks
    private Environment environment;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetAllVariables() {
        Map<String, String> variables = new HashMap<>();
        variables.put("foo", "bar");
        variables.put("baz", "qux");
        Mockito.when(environmentVariablesAccessor.getAllVariables())
               .thenReturn(variables);

        assertEquals(variables, environment.getAllVariables());
    }

    @Test
    public void testGetString() {
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn("cd");

        assertEquals("cd", environment.getString("ab"));
    }

    @Test
    public void testGetStringWhenVariableIsMissing() {
        assertNull(environment.getString("ab"));
    }

    @Test
    public void testGetStringWithDefault() {
        assertEquals("cd", environment.getString("ab", "cd"));
    }

    @Test
    public void testGetLong() {
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn("12");

        assertEquals(12, (long) environment.getLong("ab"));
    }

    @Test
    public void testGetLongWhenVariableIsMissing() {
        assertNull(environment.getLong("ab"));
    }

    @Test
    public void testGetLongWhenVariableIsInvalid() {
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn("cd");

        assertNull(environment.getLong("ab"));
    }

    @Test
    public void testGetLongWhenVariableIsInvalidWithDefault() {
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn("cd");

        assertEquals(12, (long) environment.getLong("ab", 12L));
    }

    @Test
    public void testGetLongWithDefault() {
        assertEquals(12, (long) environment.getLong("ab", 12L));
    }

    @Test
    public void testGetInteger() {
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn("12");

        assertEquals(12, (long) environment.getInteger("ab"));
    }

    @Test
    public void testGetIntegerWhenVariableIsMissing() {
        assertNull(environment.getInteger("ab"));
    }

    @Test
    public void testGetIntegerWhenVariableIsInvalid() {
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn("cd");

        assertNull(environment.getInteger("ab"));
    }

    @Test
    public void testGetIntegerWhenVariableIsInvalidWithDefault() {
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn("cd");

        assertEquals(12, (int) environment.getInteger("ab", 12));
    }

    @Test
    public void testGetIntegerWithDefault() {
        assertEquals(12, (int) environment.getInteger("ab", 12));
    }

    @Test
    public void testGetBoolean() {
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn("false");

        assertEquals(false, environment.getBoolean("ab"));
    }

    @Test
    public void testGetBooleanWhenVariableIsMissing() {
        assertNull(environment.getBoolean("ab"));
    }

    @Test
    public void testGetBooleanWhenVariableIsInvalid() {
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn("cd");

        assertEquals(false, environment.getBoolean("ab"));
    }

    @Test
    public void testGetBooleanWithDefault() {
        assertEquals(false, environment.getBoolean("ab", false));
    }

    @Test
    public void testGetPositiveIntegerWithNegativeVariable() {
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn("-1");

        assertEquals(Integer.MAX_VALUE, (int) environment.getPositiveInteger("ab", null));
    }

    @Test
    public void testGetPositiveIntegerWithPositiveVariable() {
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn("+1");

        assertEquals(+1, (int) environment.getPositiveInteger("ab", null));
    }

    @Test
    public void testGetPositiveIntegerWithZero() {
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn("0");

        assertEquals(Integer.MAX_VALUE, (int) environment.getPositiveInteger("ab", null));
    }

    @Test
    public void testGetPositiveIntegerWithNull() {
        assertEquals(Integer.MAX_VALUE, (int) environment.getPositiveInteger("ab", null));
    }

    @Test
    public void testGetNegativeIntegerWithNegativeVariable() {
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn("-1");

        assertEquals(-1, (int) environment.getNegativeInteger("ab", null));
    }

    @Test
    public void testGetNegativeIntegerWithPositiveVariable() {
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn("+1");

        assertEquals(Integer.MIN_VALUE, (int) environment.getNegativeInteger("ab", null));
    }

    @Test
    public void testGetNegativeIntegerWithZero() {
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn("0");

        assertEquals(Integer.MIN_VALUE, (int) environment.getNegativeInteger("ab", null));
    }

    @Test
    public void testGetNegativeIntegerWithNull() {
        assertEquals(Integer.MIN_VALUE, (int) environment.getNegativeInteger("ab", null));
    }

    @Test
    public void tetGetVariable() {
        UUID expectedUuid = UUID.fromString("c9fbfbfd-8d54-4cba-958f-25a3f7a14ca7");
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn(expectedUuid.toString());

        assertEquals(expectedUuid, environment.getVariable("ab", UUID::fromString));
    }

    @Test
    public void testHasVariable() {
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn("0");

        assertTrue(environment.hasVariable("ab"));
    }

    @Test
    public void testHasVariableWhenVariableIsAnEmptyString() {
        Mockito.when(environmentVariablesAccessor.getVariable("ab"))
               .thenReturn("");

        assertFalse(environment.hasVariable("ab"));
    }

    @Test
    public void testHasVariableWhenVariableIsMissing() {
        assertFalse(environment.hasVariable("ab"));
    }

}
