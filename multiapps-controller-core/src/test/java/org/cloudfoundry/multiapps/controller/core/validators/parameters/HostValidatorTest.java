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
            Arguments.of("test-test-test-test-test-test-test-test-test-test-test-test-test-test-test", false , new Expectation("test-test-test-test-test-test-test-test-test-test-test-18452d4e")),
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

    static Stream<Arguments> testValidateHostWithNamespace() {
        return Stream.of(
                         // Host with incorrect suffix
                         Arguments.of("test-incorrect-suffix", CONTEXT_APPLY_NAMESPACE, true, false, "dev", false, true, true),
                         // Host with namespace as suffix
                         Arguments.of("test-dev", Collections.emptyMap(), true, false, "dev", true, true, true),
                         Arguments.of("test-dev", Collections.emptyMap(), true, true, "dev", true, false, true),
                         Arguments.of("test-dev", Collections.emptyMap(), false, true, "dev", true, false, false),
                         // Host with namespace as suffix and -green suffix
                         Arguments.of("test-dev-green", Collections.emptyMap(), true, false, "dev", true, true, true),
                         Arguments.of("test-dev-green", Collections.emptyMap(), true, true, "dev", true, false, true),
                         Arguments.of("test-dev-green", Collections.emptyMap(), false, true, "dev", true, false, false),
                         // Host with namespace as suffix and -idle suffix
                         Arguments.of("test-dev-idle", Collections.emptyMap(), true, false, "dev", true, true, true),
                         Arguments.of("test-dev-idle", Collections.emptyMap(), true, true, "dev", true, false, true),
                         Arguments.of("test-dev-idle", Collections.emptyMap(), false, true, "dev", true, false, false),
                         // Host with namespace as suffix and -blue suffix
                         Arguments.of("test-dev-blue", Collections.emptyMap(), true, false, "dev", true, true, true),
                         Arguments.of("test-dev-blue", Collections.emptyMap(), true, true, "dev", true, false, true),
                         Arguments.of("test-dev-blue", Collections.emptyMap(), false, true, "dev", true, false, false),
                         // Operational parameter set to True
                         Arguments.of("test", CONTEXT_APPLY_NAMESPACE, true, true, "dev", false, false, false),
                         Arguments.of("test", CONTEXT_APPLY_NAMESPACE, true, false, "dev", false, false, false),
                         Arguments.of("test", CONTEXT_DO_NOT_APPLY_NAMESPACE, true, true, "dev", false, false, false),
                         Arguments.of("test", CONTEXT_DO_NOT_APPLY_NAMESPACE, true, false, "dev", false, false, false),
                         Arguments.of("test", Collections.emptyMap(), true, true, "dev", false, false, false),
                         Arguments.of("test", Collections.emptyMap(), true, false, "dev", false, false, false),
                         // Operational parameter set to False
                         Arguments.of("test", CONTEXT_APPLY_NAMESPACE, false, true, "dev", true, false, false),
                         Arguments.of("test", CONTEXT_APPLY_NAMESPACE, false, false, "dev", true, false, false),
                         Arguments.of("test", CONTEXT_DO_NOT_APPLY_NAMESPACE, false, true, "dev", true, false, false),
                         Arguments.of("test", CONTEXT_DO_NOT_APPLY_NAMESPACE, false, false, "dev", true, false, false),
                         Arguments.of("test", Collections.emptyMap(), false, true, "dev", true, false, false),
                         Arguments.of("test", Collections.emptyMap(), false, false, "dev", true, false, false),
                         // Operational parameter set to null -> Check route parameter -> Check global parameter
                         Arguments.of("test", CONTEXT_APPLY_NAMESPACE, null, true, "dev", false, false, false),
                         Arguments.of("test", CONTEXT_APPLY_NAMESPACE, null, false, "dev", false, false, false),
                         Arguments.of("test", CONTEXT_DO_NOT_APPLY_NAMESPACE, null, true, "dev", true, false, false),
                         Arguments.of("test", CONTEXT_DO_NOT_APPLY_NAMESPACE, null, false, "dev", true, false, false),
                         Arguments.of("test", Collections.emptyMap(), null, true, "dev", false, false, false),
                         Arguments.of("test", Collections.emptyMap(), null, false, "dev", true, false, false),
                         // Other cases
                         Arguments.of("dev-test", CONTEXT_APPLY_NAMESPACE, true, true, "dev", true, false, false),
                         Arguments.of("dev-test", CONTEXT_APPLY_NAMESPACE, true, false, "dev", true, false, false),
                         Arguments.of("dev-test", CONTEXT_DO_NOT_APPLY_NAMESPACE, true, true, "dev", true, false, false),
                         Arguments.of("dev-test", CONTEXT_DO_NOT_APPLY_NAMESPACE, true, false, "dev", true, false, false),
                         Arguments.of("dev-test", Collections.emptyMap(), true, true, "dev", true, false, false),
                         Arguments.of("dev-test", Collections.emptyMap(), true, false, "dev", true, false, false),
                         Arguments.of("dev-test", CONTEXT_APPLY_NAMESPACE, null, true, "dev", true, false, false),
                         Arguments.of("dev-test", CONTEXT_APPLY_NAMESPACE, null, false, "dev", true, false, false),
                         Arguments.of("test", CONTEXT_APPLY_NAMESPACE, true, true, "", true, false, false),
                         // Error in host
                         Arguments.of(false, Collections.emptyMap(), true, false, "dev", false, false, false),
                         Arguments.of(false, Collections.emptyMap(), true, true, "dev", false, false, false),
                         Arguments.of(false, Collections.emptyMap(), true, false, "dev", false, false, false));
    }

    static Stream<Arguments> testAttemptToCorrectHostWithNamespace() {
        return Stream.of(
                         // Correct host with long name and with short namespace suffix
                         Arguments.of("test-test-test-test-test-test-test-test-test-test-test-test-test-test-test", CONTEXT_APPLY_NAMESPACE,
                                      true, true, "dev", new Expectation("test-test-test-test-test-test-test-test-test-18452d4e-dev"),
                                      false, true),
                         Arguments.of("test-test-test-test-test-test-test-test-test-test-test-test-test-test-test-test-idle",
                                      CONTEXT_APPLY_NAMESPACE, true, true, "dev",
                                      new Expectation("test-test-test-test-test-test-test-test-test-2f53592c-dev-idle"), false, true),
                         Arguments.of("test-test-test-test-test-test-test-test-test-test-test-test-test-test-test-test-green",
                                      CONTEXT_APPLY_NAMESPACE, true, true, "dev",
                                      new Expectation("test-test-test-test-test-test-test-test-test-44d24803-dev-green"), false, true),
                         Arguments.of("test-test-test-test-test-test-test-test-test-test-test-test-test-test-test-test-blue",
                                      CONTEXT_APPLY_NAMESPACE, true, true, "dev",
                                      new Expectation("test-test-test-test-test-test-test-test-test-2f5668a6-dev-blue"), false, true),
                         // Correct host with long name and with long namespace suffix
                         Arguments.of("test-test-test-test-test-test-test-test-test-test-test-test-test-test-test", CONTEXT_APPLY_NAMESPACE,
                                      true, true, "dev-dev-dev-dev-dev-dev-dev-dev-devv",
                                      new Expectation("test-test-te18452d4e-dev-dev-dev-dev-dev-dev-dev-dev-devv"), false, true),
                         Arguments.of("test-test-test-test-test-test-test-test-test-test-test-test-test-test-test-test-idle",
                                      CONTEXT_APPLY_NAMESPACE, true, true, "dev-dev-dev-dev-dev-dev-dev-dev-devv",
                                      new Expectation("test-test-te2f53592c-dev-dev-dev-dev-dev-dev-dev-dev-devv-idle"), false, true),
                         Arguments.of("test-test-test-test-test-test-test-test-test-test-test-test-test-test-test-test-green",
                                      CONTEXT_APPLY_NAMESPACE, true, true, "dev-dev-dev-dev-dev-dev-dev-dev-devv",
                                      new Expectation("test-test-te44d24803-dev-dev-dev-dev-dev-dev-dev-dev-devv-green"), false, true),
                         Arguments.of("test-test-test-test-test-test-test-test-test-test-test-test-test-test-test-test-blue",
                                      CONTEXT_APPLY_NAMESPACE, true, true, "dev-dev-dev-dev-dev-dev-dev-dev-devv",
                                      new Expectation("test-test-te2f5668a6-dev-dev-dev-dev-dev-dev-dev-dev-devv-blue"), false, true),
                         // Correct host with short name and with short namespace suffix
                         Arguments.of("test-", CONTEXT_APPLY_NAMESPACE, true, true, "dev", new Expectation("test-dev"), false, true),
                         Arguments.of("test-idle", CONTEXT_APPLY_NAMESPACE, true, true, "dev", new Expectation("test-dev-idle"), false,
                                      true),
                         Arguments.of("test-green", CONTEXT_APPLY_NAMESPACE, true, true, "dev", new Expectation("test-dev-green"), false,
                                      true),
                         Arguments.of("test-blue", CONTEXT_APPLY_NAMESPACE, true, true, "dev", new Expectation("test-dev-blue"), false,
                                      true),
                         // Correct host with short name and with long namespace suffix
                         Arguments.of("test-", CONTEXT_APPLY_NAMESPACE, true, true, "dev-dev-dev-dev-dev-dev-dev-dev-devv",
                                      new Expectation("test-dev-dev-dev-dev-dev-dev-dev-dev-devv"), false, true),
                         Arguments.of("test-idle", CONTEXT_APPLY_NAMESPACE, true, true, "dev-dev-dev-dev-dev-dev-dev-dev-devv",
                                      new Expectation("test-dev-dev-dev-dev-dev-dev-dev-dev-devv-idle"), false, true),
                         Arguments.of("test-green", CONTEXT_APPLY_NAMESPACE, true, true, "dev-dev-dev-dev-dev-dev-dev-dev-devv",
                                      new Expectation("test-dev-dev-dev-dev-dev-dev-dev-dev-devv-green"), false, true),
                         Arguments.of("test-blue", CONTEXT_APPLY_NAMESPACE, true, true, "dev-dev-dev-dev-dev-dev-dev-dev-devv",
                                      new Expectation("test-dev-dev-dev-dev-dev-dev-dev-dev-devv-blue"), false, true),
                         // Operational parameter set to True
                         Arguments.of("test", CONTEXT_APPLY_NAMESPACE, true, true, "dev", new Expectation("dev-test"), false, false),
                         Arguments.of("test", CONTEXT_APPLY_NAMESPACE, true, false, "dev", new Expectation("dev-test"), false, false),
                         Arguments.of("test", CONTEXT_DO_NOT_APPLY_NAMESPACE, true, true, "dev", new Expectation("dev-test"), false, false),
                         Arguments.of("test", CONTEXT_DO_NOT_APPLY_NAMESPACE, true, false, "dev", new Expectation("dev-test"), false,
                                      false),
                         Arguments.of("test", Collections.emptyMap(), true, true, "dev", new Expectation("dev-test"), false, false),
                         Arguments.of("test", Collections.emptyMap(), true, false, "dev", new Expectation("dev-test"), false, false),
                         // Operational parameter set to False
                         Arguments.of("test", CONTEXT_APPLY_NAMESPACE, false, true, "dev", new Expectation("test"), false, false),
                         Arguments.of("test", CONTEXT_APPLY_NAMESPACE, false, false, "dev", new Expectation("test"), false, false),
                         Arguments.of("test", CONTEXT_DO_NOT_APPLY_NAMESPACE, false, true, "dev", new Expectation("test"), false, false),
                         Arguments.of("test", CONTEXT_DO_NOT_APPLY_NAMESPACE, false, false, "dev", new Expectation("test"), false, false),
                         Arguments.of("test", Collections.emptyMap(), false, true, "dev", new Expectation("test"), false, false),
                         Arguments.of("test", Collections.emptyMap(), false, false, "dev", new Expectation("test"), false, false),
                         // Operational parameter set to null -> Check route parameter -> Check global parameter
                         Arguments.of("test", CONTEXT_APPLY_NAMESPACE, null, true, "dev", new Expectation("dev-test"), false, false),
                         Arguments.of("test", CONTEXT_APPLY_NAMESPACE, null, false, "dev", new Expectation("dev-test"), false, false),
                         Arguments.of("test", CONTEXT_DO_NOT_APPLY_NAMESPACE, null, true, "dev", new Expectation("test"), false, false),
                         Arguments.of("test", CONTEXT_DO_NOT_APPLY_NAMESPACE, null, false, "dev", new Expectation("test"), false, false),
                         Arguments.of("test", Collections.emptyMap(), null, true, "dev", new Expectation("dev-test"), false, false),
                         Arguments.of("test", Collections.emptyMap(), null, false, "dev", new Expectation("test"), false, false),
                         Arguments.of("test", Collections.emptyMap(), null, true, "dev", new Expectation("dev-test"), false, false),
                         // Other cases
                         Arguments.of("test", Collections.emptyMap(), true, false, "", new Expectation("test"), false, false),
                         Arguments.of("test", CONTEXT_APPLY_NAMESPACE, false, false, "", new Expectation("test"), false, false),
                         Arguments.of("test", Collections.emptyMap(), false, true, "", new Expectation("test"), false, false),
                         Arguments.of("test", CONTEXT_APPLY_NAMESPACE, true, true, "", new Expectation("test"), false, false),

                         Arguments.of("test_application", CONTEXT_DO_NOT_APPLY_NAMESPACE, false, true, "prod",
                                      new Expectation("test-application"), false, false),
                         Arguments.of("test_application", CONTEXT_DO_NOT_APPLY_NAMESPACE, true, false, "prod",
                                      new Expectation("prod-test-application"), false, false),
                         Arguments.of(false, Collections.emptyMap(), true, true, "dev",
                                      new Expectation(Expectation.Type.EXCEPTION, "Could not create a valid host from \"false\""), false,
                                      false),
                         Arguments.of(false, Collections.emptyMap(), true, false, "dev",
                                      new Expectation(Expectation.Type.EXCEPTION, "Could not create a valid host from \"false\""), false,
                                      false));
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

    @ParameterizedTest
    @MethodSource
    void testValidateHostWithNamespace(Object host, Map<String, Object> context, Boolean applyNamespaceProcessVariable,
                                       boolean applyNamespaceGlobalLevel, String namespace, boolean expectedValidHost,
                                       boolean applyNamespaceAsSuffixGlobalLevel, Boolean applyNamespaceAsSuffixProcessVariable) {
        HostValidator hostValidator = new HostValidator(namespace,
                                                        applyNamespaceGlobalLevel,
                                                        applyNamespaceProcessVariable,
                                                        applyNamespaceAsSuffixGlobalLevel,
                                                        applyNamespaceAsSuffixProcessVariable);
        boolean result = hostValidator.isValid(host, context);
        assertEquals(expectedValidHost, result);
    }

    @ParameterizedTest
    @MethodSource
    void testAttemptToCorrectHostWithNamespace(Object host, Map<String, Object> context, Boolean applyNamespaceProcessVariable,
                                               boolean applyNamespaceGlobalLevel, String namespace, Expectation expectation,
                                               boolean applyNamespaceAsSuffixGlobalLevel, Boolean applyNamespaceAsSuffixProcessVariable) {
        HostValidator hostValidator = new HostValidator(namespace,
                                                        applyNamespaceGlobalLevel,
                                                        applyNamespaceProcessVariable,
                                                        applyNamespaceAsSuffixGlobalLevel,
                                                        applyNamespaceAsSuffixProcessVariable);
        tester.test(() -> hostValidator.attemptToCorrect(host, context), expectation);
    }

}
