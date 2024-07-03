package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.core.Constants;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class ApplicationNameValidatorTest {

    private static final String NAMESPACE = "foo";
    private static final String APPLICATION_NAME = "bar";
    private static final Map<String, Object> CONTEXT_APPLY_NAMESPACE = Map.of(SupportedParameters.APPLY_NAMESPACE, new Boolean(true));
    private static final Map<String, Object> CONTEXT_DO_NOT_APPLY_NAMESPACE = Map.of(SupportedParameters.APPLY_NAMESPACE,
                                                                                     new Boolean(false));

    public static Stream<Arguments> getParameters() {
        return Stream.of(Arguments.of(true, CONTEXT_APPLY_NAMESPACE, true), Arguments.of(true, CONTEXT_APPLY_NAMESPACE, false),
                         Arguments.of(true, CONTEXT_DO_NOT_APPLY_NAMESPACE, true),
                         Arguments.of(true, CONTEXT_DO_NOT_APPLY_NAMESPACE, false), Arguments.of(true, Collections.emptyMap(), true),
                         Arguments.of(true, Collections.emptyMap(), false), Arguments.of(false, CONTEXT_APPLY_NAMESPACE, true),
                         Arguments.of(false, CONTEXT_APPLY_NAMESPACE, false), Arguments.of(false, CONTEXT_DO_NOT_APPLY_NAMESPACE, true),
                         Arguments.of(false, CONTEXT_DO_NOT_APPLY_NAMESPACE, false), Arguments.of(false, Collections.emptyMap(), true),
                         Arguments.of(false, Collections.emptyMap(), false), Arguments.of(null, CONTEXT_APPLY_NAMESPACE, true),
                         Arguments.of(null, CONTEXT_APPLY_NAMESPACE, false), Arguments.of(null, CONTEXT_DO_NOT_APPLY_NAMESPACE, true),
                         Arguments.of(null, CONTEXT_DO_NOT_APPLY_NAMESPACE, false), Arguments.of(null, Collections.emptyMap(), true),
                         Arguments.of(null, Collections.emptyMap(), false));
    }

    @ParameterizedTest
    @MethodSource("getParameters")
    void testCorrectionWithNoNamespaces(Boolean applyNamespaceProcessVariable, Map<String, Object> context,
                                        boolean applyNamespaceGlobalLevel) {
        ApplicationNameValidator applicationNameValidator = new ApplicationNameValidator(null,
                                                                                         applyNamespaceGlobalLevel,
                                                                                         applyNamespaceProcessVariable);
        String result = (String) applicationNameValidator.attemptToCorrect(APPLICATION_NAME, context);
        assertEquals(APPLICATION_NAME, result);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void testCorrectionWithModuleLevelParameterSetToTrue(boolean applyNamespaceGlobalLevel) {
        ApplicationNameValidator applicationNameValidator = new ApplicationNameValidator(NAMESPACE, applyNamespaceGlobalLevel, null);
        String result = (String) applicationNameValidator.attemptToCorrect(APPLICATION_NAME, CONTEXT_APPLY_NAMESPACE);
        assertEquals(String.format("%s" + Constants.NAMESPACE_SEPARATOR + "%s", NAMESPACE, APPLICATION_NAME), result);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void testCorrectionWithModuleLevelParameterSetToFalse(boolean applyNamespaceGlobalLevel) {
        ApplicationNameValidator applicationNameValidator = new ApplicationNameValidator(NAMESPACE, applyNamespaceGlobalLevel, null);
        String result = (String) applicationNameValidator.attemptToCorrect(APPLICATION_NAME, CONTEXT_DO_NOT_APPLY_NAMESPACE);
        assertEquals(APPLICATION_NAME, result);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void testCorrectionWithGlobalLevelParameter(boolean applyNamespaceGlobalLevel) {
        ApplicationNameValidator applicationNameValidator = new ApplicationNameValidator(NAMESPACE, applyNamespaceGlobalLevel, null);
        String result = (String) applicationNameValidator.attemptToCorrect(APPLICATION_NAME, Collections.emptyMap());
        if (applyNamespaceGlobalLevel) {
            assertEquals(String.format("%s" + Constants.NAMESPACE_SEPARATOR + "%s", NAMESPACE, APPLICATION_NAME), result);
        } else {
            assertEquals(APPLICATION_NAME, result);
        }
    }

    public static Stream<Arguments> getGlobalAndModuleLevelNamespaceParameters() {
        return Stream.of(Arguments.of(true, CONTEXT_APPLY_NAMESPACE), Arguments.of(true, CONTEXT_DO_NOT_APPLY_NAMESPACE),
                         Arguments.of(true, Collections.emptyMap()), Arguments.of(false, CONTEXT_APPLY_NAMESPACE),
                         Arguments.of(false, CONTEXT_DO_NOT_APPLY_NAMESPACE), Arguments.of(false, Collections.emptyMap()));
    }

    @ParameterizedTest
    @MethodSource("getGlobalAndModuleLevelNamespaceParameters")
    void testCorrectionWithProcessVariableSetToTrue(boolean applyNamespaceGlobalLevel, Map<String, Object> relatedParameters) {
        ApplicationNameValidator applicationNameValidator = new ApplicationNameValidator(NAMESPACE, applyNamespaceGlobalLevel, true);
        String result = (String) applicationNameValidator.attemptToCorrect(APPLICATION_NAME, relatedParameters);
        assertEquals(String.format("%s" + Constants.NAMESPACE_SEPARATOR + "%s", NAMESPACE, APPLICATION_NAME), result);
    }

    @ParameterizedTest
    @MethodSource("getGlobalAndModuleLevelNamespaceParameters")
    void testCorrectionWithProcessVariableSetToFalse(boolean applyNamespaceGlobalLevel, Map<String, Object> relatedParameters) {
        ApplicationNameValidator applicationNameValidator = new ApplicationNameValidator(NAMESPACE, applyNamespaceGlobalLevel, false);
        String result = (String) applicationNameValidator.attemptToCorrect(APPLICATION_NAME, relatedParameters);
        assertEquals(APPLICATION_NAME, result);
    }

    @ParameterizedTest
    @MethodSource("getGlobalAndModuleLevelNamespaceParameters")
    void testCorrectionWithEmptyNamespaceParameter(boolean applyNamespaceGlobalLevel, Map<String, Object> relatedParameters) {
        ServiceNameValidator serviceNameValidator = new ServiceNameValidator("", applyNamespaceGlobalLevel, true);
        String result = (String) serviceNameValidator.attemptToCorrect(APPLICATION_NAME, relatedParameters);
        assertEquals(APPLICATION_NAME, result);
    }

    @ParameterizedTest
    @MethodSource("getParameters")
    void testCorrectionWithInvalidApplicationName(Boolean applyNamespaceProcessVariable, Map<String, Object> context,
                                                  boolean applyNamespaceGlobalLevel) {
        ApplicationNameValidator applicationNameValidator = new ApplicationNameValidator(NAMESPACE,
                                                                                         applyNamespaceGlobalLevel,
                                                                                         applyNamespaceProcessVariable);
        assertThrows(ContentException.class, () -> applicationNameValidator.attemptToCorrect(Collections.emptyList(), context));
    }

    @ParameterizedTest
    @MethodSource("getParameters")
    void testValidation(Boolean applyNamespaceProcessVariable, Map<String, Object> context, boolean applyNamespaceGlobalLevel) {
        ApplicationNameValidator applicationNameValidator = new ApplicationNameValidator(NAMESPACE,
                                                                                         applyNamespaceGlobalLevel,
                                                                                         applyNamespaceProcessVariable);
        assertFalse(applicationNameValidator.isValid(APPLICATION_NAME, context));
    }

    public static Stream<Arguments> getGlobalParameterAndProcessVariable() {
        return Stream.of(Arguments.of(true, true), Arguments.of(true, false), Arguments.of(false, true), Arguments.of(false, false),
                         Arguments.of(null, true), Arguments.of(null, false));
    }

    @ParameterizedTest
    @MethodSource("getGlobalParameterAndProcessVariable")
    void testGetContainerType(Boolean applyNamespaceProcessVariable, boolean applyNamespaceGlobalLevel) {
        ApplicationNameValidator applicationNameValidator = new ApplicationNameValidator(NAMESPACE,
                                                                                         applyNamespaceGlobalLevel,
                                                                                         applyNamespaceProcessVariable);
        assertEquals(Module.class, applicationNameValidator.getContainerType());
    }

    @ParameterizedTest
    @MethodSource("getGlobalParameterAndProcessVariable")
    void testGetParameterName(Boolean applyNamespaceProcessVariable, boolean applyNamespaceGlobalLevel) {
        ApplicationNameValidator applicationNameValidator = new ApplicationNameValidator(NAMESPACE,
                                                                                         applyNamespaceGlobalLevel,
                                                                                         applyNamespaceProcessVariable);
        assertEquals(SupportedParameters.APP_NAME, applicationNameValidator.getParameterName());
    }

    @ParameterizedTest
    @MethodSource("getGlobalParameterAndProcessVariable")
    void testCanCorrect(Boolean applyNamespaceProcessVariable, boolean applyNamespaceGlobalLevel) {
        ApplicationNameValidator applicationNameValidator = new ApplicationNameValidator(NAMESPACE,
                                                                                         applyNamespaceGlobalLevel,
                                                                                         applyNamespaceProcessVariable);
        assertTrue(applicationNameValidator.canCorrect());
    }

}
