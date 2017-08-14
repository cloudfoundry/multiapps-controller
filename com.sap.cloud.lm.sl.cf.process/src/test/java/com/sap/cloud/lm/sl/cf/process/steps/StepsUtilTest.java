/**
 * 
 */
package com.sap.cloud.lm.sl.cf.process.steps;

import static com.sap.cloud.lm.sl.cf.process.Constants.DEFAULT_START_TIMEOUT;
import static com.sap.cloud.lm.sl.cf.process.Constants.PARAM_START_TIMEOUT;
import static com.sap.cloud.lm.sl.cf.process.Constants.VAR_START_TIME;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.activiti.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.slp.model.ParameterMetadata;
import com.sap.cloud.lm.sl.slp.model.ServiceMetadata;

public class StepsUtilTest {
    @Mock
    DelegateExecution context;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    final long currentMockTime = 19841984;
    final long threeSecondsBefore = currentMockTime - SECONDS.toMillis(10);
    final long sixtyFiveMinutesBefore = currentMockTime - MINUTES.toMillis(65);

    @Test
    public void testHasTimedOutNegative() {
        when(context.getVariable(PARAM_START_TIMEOUT)).thenReturn(DEFAULT_START_TIMEOUT);
        when(context.getVariable(VAR_START_TIME)).thenReturn(threeSecondsBefore);
        assertFalse("Test non-expired time", StepsUtil.hasTimedOut(context, () -> {
            return currentMockTime;
        }));
    }

    @Test
    public void testHasTimedOutPositive() {
        when(context.getVariable(PARAM_START_TIMEOUT)).thenReturn(DEFAULT_START_TIMEOUT);
        when(context.getVariable(VAR_START_TIME)).thenReturn(sixtyFiveMinutesBefore);
        assertTrue("Test time out", StepsUtil.hasTimedOut(context, () -> {
            return currentMockTime;
        }));
    }

    @Test
    public void testGetNonSensitiveParameters() {
        ParameterMetadata nonSensitiveParameterMetadata1 = ParameterMetadata.builder().id("nonSensitiveParameter1").build();
        ParameterMetadata nonSensitiveParameterMetadata2 = ParameterMetadata.builder().id("nonSensitiveParameter2").build();
        ParameterMetadata sensitiveParameterMetadata = ParameterMetadata.builder().id("sensitiveParameter").secure(true).build();
        ServiceMetadata serviceMetadata = ServiceMetadata.builder().parameters(new HashSet<>(
            Arrays.asList(nonSensitiveParameterMetadata1, nonSensitiveParameterMetadata2, sensitiveParameterMetadata))).build();
        Map<String, Object> variables = new HashMap<>();
        variables.put("nonSensitiveParameter1", "foo");
        variables.put("nonSensitiveParameter2", "bar");
        variables.put("sensitiveParameter", "baz");
        when(context.getVariables()).thenReturn(variables);
        Map<String, Object> nonSensitiveVariables = StepsUtil.getNonSensitiveVariables(context, serviceMetadata);
        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("nonSensitiveParameter1", "foo");
        expectedResult.put("nonSensitiveParameter2", "bar");
        assertEquals(expectedResult, nonSensitiveVariables);
    }

}
