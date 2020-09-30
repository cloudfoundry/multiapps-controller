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

class HostValidatorTest {

    private final Tester tester = Tester.forClass(getClass());

    private final HostValidator validator = new HostValidator();

    public static Stream<Arguments> getParameters() {
        return Stream.of(
// @formatter:off
            // (0)
            Arguments.of("TEST_TEST_TEST", false, new Expectation("test-test-test")),
            // (1)
            Arguments.of("test-test-test", true , new Expectation("test-test-test")),
            // (2)
            Arguments.of("---", false, new Expectation(Expectation.Type.EXCEPTION, "Could not create a valid host from \"---\"")),
            // (3)
            Arguments.of("@12", false, new Expectation("12")),
            // (4)
            Arguments.of("@@@", false, new Expectation(Expectation.Type.EXCEPTION, "Could not create a valid host from \"@@@\""))
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource("getParameters")
    void testValidate(String host, boolean isValid, Expectation expectation) {
        assertEquals(isValid, validator.isValid(host, null));
    }

    @Test
    void testCanCorrect() {
        assertTrue(validator.canCorrect());
    }

    @ParameterizedTest
    @MethodSource("getParameters")
    void testAttemptToCorrect(String host, boolean isValid, Expectation expectation) {
        tester.test(() -> validator.attemptToCorrect(host, null), expectation);
    }

    @Test
    void testGetParameterName() {
        assertEquals("host", validator.getParameterName());
    }

    @Test
    void testGetContainerType() {
        assertTrue(validator.getContainerType()
                            .isAssignableFrom(Module.class));
    }

}
