/**
 * 
 */
package com.sap.cloud.lm.sl.cf.process.steps;

import static com.sap.cloud.lm.sl.cf.process.Constants.DEFAULT_START_TIMEOUT;
import static com.sap.cloud.lm.sl.cf.process.Constants.PARAM_START_TIMEOUT;
import static com.sap.cloud.lm.sl.cf.process.Constants.VAR_START_TIME;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.activiti.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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
}
