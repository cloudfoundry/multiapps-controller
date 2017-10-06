package com.sap.cloud.lm.sl.cf.core.validators.parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.mta.model.v1_0.Module;

@RunWith(Parameterized.class)
public class DomainValidatorTest {

    private DomainValidator validator = new DomainValidator();

    private boolean isValid;
    private String domain;
    private String expected;

    public DomainValidatorTest(String domain, boolean isValid, String expected) {
        this.isValid = isValid;
        this.domain = domain;
        this.expected = expected;
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0)
            { "TEST_TEST_TEST", false, "test-test-test", },
            // (1)
            { "test-test-test", true , "test-test-test", },
            // (2)
            { "test.test.test", true , "test.test.test", },
            // (3)
            { "---", false, "E:Could not create a valid domain from \"---\"", },
            // (4)
            { "@12", false, "12", },
            // (5)
            { "@@@", false, "E:Could not create a valid domain from \"@@@\"", },
// @formatter:on
        });
    }

    @Test
    public void testValidate() {
        assertEquals(isValid, validator.isValid(domain));
    }

    @Test
    public void testCanCorrect() {
        assertTrue(validator.canCorrect());
    }

    @Test
    public void testAttemptToCorrect() throws Exception {
        TestUtil.test(() -> validator.attemptToCorrect(domain), expected, getClass(), false);
    }

    @Test
    public void testGetParameterName() {
        assertEquals("domain", validator.getParameterName());
    }

    @Test
    public void testGetContainerType() {
        assertTrue(validator.getContainerType().isAssignableFrom(Module.class));
    }
}
