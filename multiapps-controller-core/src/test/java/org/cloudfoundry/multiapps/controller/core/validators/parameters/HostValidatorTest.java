package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.test.Tester;
import org.cloudfoundry.multiapps.common.test.Tester.Expectation;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
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
            Arguments.of("@@@", false, new Expectation(Expectation.Type.EXCEPTION, "Could not create a valid host from \"@@@\"")),
            // (5)
            Arguments.of(false, false, new Expectation(Expectation.Type.EXCEPTION, "Could not create a valid host from \"false\""))
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource("getParameters")
    void testValidate(Object host, boolean isValid, Expectation expectation) {
        assertEquals(isValid, validator.isValid(host, Collections.emptyMap()));
    }

    @Test
    void testCanCorrect() {
        assertTrue(validator.canCorrect());
    }

    @ParameterizedTest
    @MethodSource("getParameters")
    void testAttemptToCorrect(Object host, boolean isValid, Expectation expectation) {
        tester.test(() -> validator.attemptToCorrect(host, Collections.emptyMap()), expectation);
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

    static Stream<Arguments> testValidateHostWithNamespace() {
        return Stream.of(Arguments.of("test", Collections.emptyMap(), true, "dev", false),
                         Arguments.of("dev-test", Collections.emptyMap(), true, "dev", true),
                         Arguments.of("prod-test-application", Map.of(SupportedParameters.APPLY_NAMESPACE, true), true, "prod", true),
                         Arguments.of("test-application", Map.of(SupportedParameters.APPLY_NAMESPACE, false), true, "prod", true),
                         Arguments.of(false, Collections.emptyMap(), true, "dev", false),
                         Arguments.of("test", Collections.emptyMap(), false, "dev", true));
    }

    @ParameterizedTest
    @MethodSource
    void testValidateHostWithNamespace(Object host, Map<String, Object> context, boolean applyNamespaceGlobal, String namespace,
                                       boolean expectedValidHost) {
        HostValidator hostValidator = new HostValidator(namespace, applyNamespaceGlobal);
        boolean result = hostValidator.isValid(host, context);
        assertEquals(expectedValidHost, result);
    }

    static Stream<Arguments> testAttemptToCorrectHostWithNamespace() {
        return Stream.of(Arguments.of("test", Collections.emptyMap(), true, "dev", new Expectation("dev-test")),
                         Arguments.of("test_application", Map.of(SupportedParameters.APPLY_NAMESPACE, false), true, "prod",
                                      new Expectation("test-application")),
                         Arguments.of("test-application", Map.of(SupportedParameters.APPLY_NAMESPACE, true), true, "prod",
                                      new Expectation("prod-test-application")),
                         Arguments.of(false, Collections.emptyMap(), true, "dev",
                                      new Expectation(Expectation.Type.EXCEPTION, "Could not create a valid host from \"false\"")),
                         Arguments.of("test", Collections.emptyMap(), false, "dev", new Expectation("test")));
    }

    @ParameterizedTest
    @MethodSource
    void testAttemptToCorrectHostWithNamespace(Object host, Map<String, Object> context, boolean applyNamespaceGlobal, String namespace,
                                               Expectation expectation) {
        HostValidator hostValidator = new HostValidator(namespace, applyNamespaceGlobal);
        tester.test(() -> hostValidator.attemptToCorrect(host, context), expectation);
    }

}
