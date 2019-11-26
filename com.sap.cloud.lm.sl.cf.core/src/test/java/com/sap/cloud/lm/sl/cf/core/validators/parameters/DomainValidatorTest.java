package com.sap.cloud.lm.sl.cf.core.validators.parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.common.util.Tester;
import com.sap.cloud.lm.sl.common.util.Tester.Expectation;
import com.sap.cloud.lm.sl.mta.model.Module;

@RunWith(Parameterized.class)
public class DomainValidatorTest {

    private final Tester tester = Tester.forClass(getClass());

    private final DomainValidator validator = new DomainValidator();

    private final boolean isValid;
    private final String domain;
    private final Expectation expectation;

    public DomainValidatorTest(String domain, boolean isValid, Expectation expectation) {
        this.isValid = isValid;
        this.domain = domain;
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
            { "test.test.test", true , new Expectation("test.test.test"), },
            // (3)
            { "---", false, new Expectation(Expectation.Type.EXCEPTION, "Could not create a valid domain from \"---\"") },
            // (4)
            { "@12", false, new Expectation("12"), },
            // (5)
            { "@@@", false, new Expectation(Expectation.Type.EXCEPTION, "Could not create a valid domain from \"@@@\"") },
// @formatter:on
        });
    }

    @Test
    public void testValidate() {
        assertEquals(isValid, validator.isValid(domain, null));
    }

    @Test
    public void testCanCorrect() {
        assertTrue(validator.canCorrect());
    }

    @Test
    public void testAttemptToCorrect() {
        tester.test(() -> validator.attemptToCorrect(domain, null), expectation);
    }

    @Test
    public void testGetParameterName() {
        assertEquals("domain", validator.getParameterName());
    }

    @Test
    public void testGetContainerType() {
        assertTrue(validator.getContainerType()
                            .isAssignableFrom(Module.class));
    }
}
