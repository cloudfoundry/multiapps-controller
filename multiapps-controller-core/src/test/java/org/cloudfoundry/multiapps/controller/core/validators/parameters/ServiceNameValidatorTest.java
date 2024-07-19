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
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class ServiceNameValidatorTest {

    private static final String NAMESPACE = "foo";
    private static final String SERVICE_NAME = "bar";
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
    void testCorrectionWithNoNamespaces(Boolean applyNamespaceOperational, Map<String, Object> context, boolean applyNamespaceGlobal) {
        ServiceNameValidator serviceNameValidator = new ServiceNameValidator(null, applyNamespaceGlobal, applyNamespaceOperational);
        String result = (String) serviceNameValidator.attemptToCorrect(SERVICE_NAME, context);
        assertEquals(SERVICE_NAME, result);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void testCorrectionWithNamespacesResourceParameterSetToTrue(boolean applyNamespaceGlobal) {
        ServiceNameValidator serviceNameValidator = new ServiceNameValidator(NAMESPACE, applyNamespaceGlobal, null);
        String result = (String) serviceNameValidator.attemptToCorrect(SERVICE_NAME, CONTEXT_APPLY_NAMESPACE);
        assertEquals(String.format("%s" + Constants.NAMESPACE_SEPARATOR + "%s", NAMESPACE, SERVICE_NAME), result);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void testCorrectionWithNamespacesResourceParameterSetToFalse(boolean applyNamespaceGlobal) {
        ServiceNameValidator serviceNameValidator = new ServiceNameValidator(NAMESPACE, applyNamespaceGlobal, null);
        String result = (String) serviceNameValidator.attemptToCorrect(SERVICE_NAME, CONTEXT_DO_NOT_APPLY_NAMESPACE);
        assertEquals(SERVICE_NAME, result);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void testCorrectionWithGlobalNamespaceParameter(boolean applyNamespaceGlobal) {
        ServiceNameValidator serviceNameValidator = new ServiceNameValidator(NAMESPACE, applyNamespaceGlobal, null);
        String result = (String) serviceNameValidator.attemptToCorrect(SERVICE_NAME, Collections.emptyMap());
        if (applyNamespaceGlobal) {
            assertEquals(String.format("%s" + Constants.NAMESPACE_SEPARATOR + "%s", NAMESPACE, SERVICE_NAME), result);
        } else {
            assertEquals(SERVICE_NAME, result);
        }
    }

    public static Stream<Arguments> getGlobalAndResourceNamespaceParameters() {
        return Stream.of(Arguments.of(true, CONTEXT_APPLY_NAMESPACE), Arguments.of(true, CONTEXT_DO_NOT_APPLY_NAMESPACE),
                         Arguments.of(true, Collections.emptyMap()), Arguments.of(false, CONTEXT_APPLY_NAMESPACE),
                         Arguments.of(false, CONTEXT_DO_NOT_APPLY_NAMESPACE), Arguments.of(false, Collections.emptyMap()));
    }

    @ParameterizedTest
    @MethodSource("getGlobalAndResourceNamespaceParameters")
    void testCorrectionWithOperationNamespaceParameterSetToTrue(boolean applyNamespaceGlobal, Map<String, Object> relatedParameters) {
        ServiceNameValidator serviceNameValidator = new ServiceNameValidator(NAMESPACE, applyNamespaceGlobal, true);
        String result = (String) serviceNameValidator.attemptToCorrect(SERVICE_NAME, relatedParameters);
        assertEquals(String.format("%s" + Constants.NAMESPACE_SEPARATOR + "%s", NAMESPACE, SERVICE_NAME), result);
    }

    @ParameterizedTest
    @MethodSource("getGlobalAndResourceNamespaceParameters")
    void testCorrectionWithOperationNamespaceParameterSetToFalse(boolean applyNamespaceGlobal, Map<String, Object> relatedParameters) {
        ServiceNameValidator serviceNameValidator = new ServiceNameValidator(NAMESPACE, applyNamespaceGlobal, false);
        String result = (String) serviceNameValidator.attemptToCorrect(SERVICE_NAME, relatedParameters);
        assertEquals(SERVICE_NAME, result);
    }

    @ParameterizedTest
    @MethodSource("getParameters")
    void testCorrectionWithInvalidServiceName(Boolean applyNamespaceOperational, Map<String, Object> context,
                                              boolean applyNamespaceGlobal) {
        ServiceNameValidator serviceNameValidator = new ServiceNameValidator(NAMESPACE, applyNamespaceGlobal, applyNamespaceOperational);
        assertThrows(ContentException.class, () -> serviceNameValidator.attemptToCorrect(Collections.emptyList(), context));
    }

    @ParameterizedTest
    @MethodSource("getParameters")
    void testValidation(Boolean applyNamespaceOperational, Map<String, Object> context, boolean applyNamespaceGlobal) {
        ServiceNameValidator serviceNameValidator = new ServiceNameValidator(NAMESPACE, applyNamespaceGlobal, applyNamespaceOperational);
        assertFalse(serviceNameValidator.isValid(SERVICE_NAME, context));
    }

    public static Stream<Arguments> getGlobalAndOperationalParameters() {
        return Stream.of(Arguments.of(true, true), Arguments.of(true, false), Arguments.of(false, true), Arguments.of(false, false),
                         Arguments.of(null, true), Arguments.of(null, false));
    }

    @ParameterizedTest
    @MethodSource("getGlobalAndOperationalParameters")
    void testGetContainerType(Boolean applyNamespaceOperational, boolean applyNamespaceGlobal) {
        ServiceNameValidator serviceNameValidator = new ServiceNameValidator(NAMESPACE, applyNamespaceGlobal, applyNamespaceOperational);
        assertEquals(Resource.class, serviceNameValidator.getContainerType());
    }

    @ParameterizedTest
    @MethodSource("getGlobalAndOperationalParameters")
    void testGetParameterName(Boolean applyNamespaceOperational, boolean applyNamespaceGlobal) {
        ServiceNameValidator serviceNameValidator = new ServiceNameValidator(NAMESPACE, applyNamespaceGlobal, applyNamespaceOperational);
        assertEquals(SupportedParameters.SERVICE_NAME, serviceNameValidator.getParameterName());
    }

    @ParameterizedTest
    @MethodSource("getGlobalAndOperationalParameters")
    void testCanCorrect(Boolean applyNamespaceOperational, boolean applyNamespaceGlobal) {
        ServiceNameValidator serviceNameValidator = new ServiceNameValidator(NAMESPACE, applyNamespaceGlobal, applyNamespaceOperational);
        assertTrue(serviceNameValidator.canCorrect());
    }

}
