package com.sap.cloud.lm.sl.cf.core.validators.parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.mta.model.Module;

@RunWith(Parameterized.class)
public class RouteValidatorTest {

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // [0]
            {"valid-host.domain", true, "valid-host.domain", null},
            // [1]
            {"should_correct$this$host.domain", false, "should-correct-this-host.domain", null},
            // [2]
            {"host_can_be_corrected.domain.com", false, "host-can-be-corrected.domain.com", null},
// @formatter:on
        });
    }

    private static final RouteValidator validator = new RouteValidator();

    private final String inputRoute;
    private final boolean isValid;
    private final String correctedRoute;
    private final String expectedException;

    public RouteValidatorTest(String inputRoute, boolean isValid, String correctedRoute, String expectedException) {
        this.inputRoute = inputRoute;
        this.isValid = isValid;
        this.correctedRoute = correctedRoute;
        this.expectedException = expectedException;
    }

    @Test
    public void testValidate() {
        assertEquals(isValid, validator.isValid(inputRoute, null));
    }

    @Test
    public void testCanCorrect() {
        assertTrue(validator.canCorrect());
    }

    @Test
    public void testAttemptToCorrect() {
        if (!validator.canCorrect())
            return;

        try {
            String result = validator.attemptToCorrect(inputRoute, null);
            assertEquals(correctedRoute, result);
        } catch (Exception e) {
            assertNotNull(e.getMessage(), expectedException);
            assertThat("Exception's message doesn't match up!", e.getMessage(), CoreMatchers.containsString(expectedException));
        }
    }

    @Test
    public void testGetParameterName() {
        assertEquals("route", validator.getParameterName());
    }

    @Test
    public void testGetContainerType() {
        assertTrue(validator.getContainerType()
                            .isAssignableFrom(Module.class));
    }

}
