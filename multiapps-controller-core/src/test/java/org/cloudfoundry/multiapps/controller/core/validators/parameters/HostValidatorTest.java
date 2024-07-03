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
    private static final Map<String, Object> CONTEXT_APPLY_NAMESPACE = Map.of(SupportedParameters.APPLY_NAMESPACE, new Boolean(true));

    private static final Map<String, Object> CONTEXT_DO_NOT_APPLY_NAMESPACE = Map.of(SupportedParameters.APPLY_NAMESPACE,
                                                                                     new Boolean(false));

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
        return Stream.of(
                         // Operational parameter set to True
                         Arguments.of("test", CONTEXT_APPLY_NAMESPACE, true, true, "dev", false),
                         Arguments.of("test", CONTEXT_APPLY_NAMESPACE, true, false, "dev", false),
                         Arguments.of("test", CONTEXT_DO_NOT_APPLY_NAMESPACE, true, true, "dev", false),
                         Arguments.of("test", CONTEXT_DO_NOT_APPLY_NAMESPACE, true, false, "dev", false),
                         Arguments.of("test", Collections.emptyMap(), true, true, "dev", false),
                         Arguments.of("test", Collections.emptyMap(), true, false, "dev", false),
                         // Operational parameter set to False
                         Arguments.of("test", CONTEXT_APPLY_NAMESPACE, false, true, "dev", true),
                         Arguments.of("test", CONTEXT_APPLY_NAMESPACE, false, false, "dev", true),
                         Arguments.of("test", CONTEXT_DO_NOT_APPLY_NAMESPACE, false, true, "dev", true),
                         Arguments.of("test", CONTEXT_DO_NOT_APPLY_NAMESPACE, false, false, "dev", true),
                         Arguments.of("test", Collections.emptyMap(), false, true, "dev", true),
                         Arguments.of("test", Collections.emptyMap(), false, false, "dev", true),
                         // Operational parameter set to null -> Check route parameter -> Check global parameter
                         Arguments.of("test", CONTEXT_APPLY_NAMESPACE, null, true, "dev", false),
                         Arguments.of("test", CONTEXT_APPLY_NAMESPACE, null, false, "dev", false),
                         Arguments.of("test", CONTEXT_DO_NOT_APPLY_NAMESPACE, null, true, "dev", true),
                         Arguments.of("test", CONTEXT_DO_NOT_APPLY_NAMESPACE, null, false, "dev", true),
                         Arguments.of("test", Collections.emptyMap(), null, true, "dev", false),
                         Arguments.of("test", Collections.emptyMap(), null, false, "dev", true),
                         // Other cases
                         Arguments.of("dev-test", CONTEXT_APPLY_NAMESPACE, true, true, "dev", true),
                         Arguments.of("dev-test", CONTEXT_APPLY_NAMESPACE, true, false, "dev", true),
                         Arguments.of("dev-test", CONTEXT_DO_NOT_APPLY_NAMESPACE, true, true, "dev", true),
                         Arguments.of("dev-test", CONTEXT_DO_NOT_APPLY_NAMESPACE, true, false, "dev", true),
                         Arguments.of("dev-test", Collections.emptyMap(), true, true, "dev", true),
                         Arguments.of("dev-test", Collections.emptyMap(), true, false, "dev", true),
                         Arguments.of("dev-test", CONTEXT_APPLY_NAMESPACE, null, true, "dev", true),
                         Arguments.of("dev-test", CONTEXT_APPLY_NAMESPACE, null, false, "dev", true),
                         Arguments.of("test", CONTEXT_APPLY_NAMESPACE, true, true, "", true),
                         // Error in host
                         Arguments.of(false, Collections.emptyMap(), true, false, "dev", false),
                         Arguments.of(false, Collections.emptyMap(), true, true, "dev", false),
                         Arguments.of(false, Collections.emptyMap(), true, false, "dev", false));
    }

    @ParameterizedTest
    @MethodSource
    void testValidateHostWithNamespace(Object host, Map<String, Object> context, Boolean applyNamespaceProcessVariable,
                                       boolean applyNamespaceGlobalLevel, String namespace, boolean expectedValidHost) {
        HostValidator hostValidator = new HostValidator(namespace, applyNamespaceGlobalLevel, applyNamespaceProcessVariable);
        boolean result = hostValidator.isValid(host, context);
        assertEquals(expectedValidHost, result);
    }

    static Stream<Arguments> testAttemptToCorrectHostWithNamespace() {
        return Stream.of(
                         // Operational parameter set to True
                         Arguments.of("test", CONTEXT_APPLY_NAMESPACE, true, true, "dev", new Expectation("dev-test")),
                         Arguments.of("test", CONTEXT_APPLY_NAMESPACE, true, false, "dev", new Expectation("dev-test")),
                         Arguments.of("test", CONTEXT_DO_NOT_APPLY_NAMESPACE, true, true, "dev", new Expectation("dev-test")),
                         Arguments.of("test", CONTEXT_DO_NOT_APPLY_NAMESPACE, true, false, "dev", new Expectation("dev-test")),
                         Arguments.of("test", Collections.emptyMap(), true, true, "dev", new Expectation("dev-test")),
                         Arguments.of("test", Collections.emptyMap(), true, false, "dev", new Expectation("dev-test")),
                         // Operational parameter set to False
                         Arguments.of("test", CONTEXT_APPLY_NAMESPACE, false, true, "dev", new Expectation("test")),
                         Arguments.of("test", CONTEXT_APPLY_NAMESPACE, false, false, "dev", new Expectation("test")),
                         Arguments.of("test", CONTEXT_DO_NOT_APPLY_NAMESPACE, false, true, "dev", new Expectation("test")),
                         Arguments.of("test", CONTEXT_DO_NOT_APPLY_NAMESPACE, false, false, "dev", new Expectation("test")),
                         Arguments.of("test", Collections.emptyMap(), false, true, "dev", new Expectation("test")),
                         Arguments.of("test", Collections.emptyMap(), false, false, "dev", new Expectation("test")),
                         // Operational parameter set to null -> Check route parameter -> Check global parameter
                         Arguments.of("test", CONTEXT_APPLY_NAMESPACE, null, true, "dev", new Expectation("dev-test")),
                         Arguments.of("test", CONTEXT_APPLY_NAMESPACE, null, false, "dev", new Expectation("dev-test")),
                         Arguments.of("test", CONTEXT_DO_NOT_APPLY_NAMESPACE, null, true, "dev", new Expectation("test")),
                         Arguments.of("test", CONTEXT_DO_NOT_APPLY_NAMESPACE, null, false, "dev", new Expectation("test")),
                         Arguments.of("test", Collections.emptyMap(), null, true, "dev", new Expectation("dev-test")),
                         Arguments.of("test", Collections.emptyMap(), null, false, "dev", new Expectation("test")),
                         Arguments.of("test", Collections.emptyMap(), null, true, "dev", new Expectation("dev-test")),
                         // Other cases
                         Arguments.of("test", Collections.emptyMap(), true, false, "", new Expectation("test")),
                         Arguments.of("test", CONTEXT_APPLY_NAMESPACE, false, false, "", new Expectation("test")),
                         Arguments.of("test", Collections.emptyMap(), false, true, "", new Expectation("test")),
                         Arguments.of("test", CONTEXT_APPLY_NAMESPACE, true, true, "", new Expectation("test")),

                         Arguments.of("test_application", CONTEXT_DO_NOT_APPLY_NAMESPACE, false, true, "prod",
                                      new Expectation("test-application")),
                         Arguments.of("test_application", CONTEXT_DO_NOT_APPLY_NAMESPACE, true, false, "prod",
                                      new Expectation("prod-test-application")),
                         Arguments.of(false, Collections.emptyMap(), true, true, "dev",
                                      new Expectation(Expectation.Type.EXCEPTION, "Could not create a valid host from \"false\"")),
                         Arguments.of(false, Collections.emptyMap(), true, false, "dev",
                                      new Expectation(Expectation.Type.EXCEPTION, "Could not create a valid host from \"false\"")));
    }

    @ParameterizedTest
    @MethodSource
    void testAttemptToCorrectHostWithNamespace(Object host, Map<String, Object> context, Boolean applyNamespaceProcessVariable,
                                               boolean applyNamespaceGlobalLevel, String namespace, Expectation expectation) {
        HostValidator hostValidator = new HostValidator(namespace, applyNamespaceGlobalLevel, applyNamespaceProcessVariable);
        tester.test(() -> hostValidator.attemptToCorrect(host, context), expectation);
    }

}
