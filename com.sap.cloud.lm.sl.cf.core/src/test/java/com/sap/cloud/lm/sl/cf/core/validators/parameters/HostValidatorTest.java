package com.sap.cloud.lm.sl.cf.core.validators.parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil.Expectation;
import com.sap.cloud.lm.sl.mta.model.v1.Module;

@RunWith(Parameterized.class)
public class HostValidatorTest {

    private HostValidator validator = new HostValidator();

    private boolean isValid;
    private String host;
    private Expectation expectation;

    public HostValidatorTest(String host, boolean isValid, Expectation expectation) {
        this.isValid = isValid;
        this.host = host;
        this.expectation = expectation;
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0)
            { "TEST_TEST_TEST", false, new Expectation("test-test-test"), },
            // (1)
            { "test-test-test", true , new Expectation("test-test-test"), },
            // (2)
            { "---", false, new Expectation(Expectation.Type.EXCEPTION, "Could not create a valid host from \"---\"") },
            // (3)
            { "@12", false, new Expectation("12"), },
            // (4)
            { "@@@", false, new Expectation(Expectation.Type.EXCEPTION, "Could not create a valid host from \"@@@\"") },
// @formatter:on
        });
    }

    @Test
    public void testValidate() {
        assertEquals(isValid, validator.isValid(host));
    }

    @Test
    public void testCanCorrect() {
        assertTrue(validator.canCorrect());
    }

    @Test
    public void testAttemptToCorrect() throws Exception {
        TestUtil.test(() -> validator.attemptToCorrect(host), expectation, getClass());
    }

    @Test
    public void testGetParameterName() {
        assertEquals("host", validator.getParameterName());
    }

    @Test
    public void testGetContainerType() {
        assertTrue(validator.getContainerType()
            .isAssignableFrom(Module.class));
    }

}
