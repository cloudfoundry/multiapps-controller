package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.test.Tester;
import org.cloudfoundry.multiapps.common.test.Tester.Expectation;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DomainValidatorTest {

    private final Tester tester = Tester.forClass(getClass());

    private final DomainValidator validator = new DomainValidator();

    public static Stream<Arguments> getParameters() {
        return Stream.of(
// @formatter:off
            // (0)
            Arguments.of("TEST_TEST_TEST", false, new Expectation("test-test-test")),
            // (1)
            Arguments.of("test-test-test", true , new Expectation("test-test-test")),
            // (2)
            Arguments.of("test.test.test", true , new Expectation("test.test.test")),
            // (3)
            Arguments.of("---", false, new Expectation(Expectation.Type.EXCEPTION, "Could not create a valid domain from \"---\"")),
            // (4)
            Arguments.of("@12", false, new Expectation("12")),
            // (5)
            Arguments.of("@@@", false, new Expectation(Expectation.Type.EXCEPTION, "Could not create a valid domain from \"@@@\""))
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource("getParameters")
    void testValidate(String domain, boolean isValid, Expectation expectation) {
        assertEquals(isValid, validator.isValid(domain, null));
    }

    @Test
    void testCanCorrect() {
        assertTrue(validator.canCorrect());
    }

    @ParameterizedTest
    @MethodSource("getParameters")
    void testAttemptToCorrect(String domain, boolean isValid, Expectation expectation) {
        tester.test(() -> validator.attemptToCorrect(domain, null), expectation);
    }

    @Test
    void testGetParameterName() {
        assertEquals("domain", validator.getParameterName());
    }

    @Test
    void testGetContainerType() {
        assertTrue(validator.getContainerType()
                            .isAssignableFrom(Module.class));
    }
}
