package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import static org.cloudfoundry.multiapps.controller.core.model.SupportedParameters.NO_HOSTNAME;
import static org.cloudfoundry.multiapps.controller.core.util.TestData.routeParameter;
import static org.cloudfoundry.multiapps.controller.core.util.TestData.routeParameterWithAdditionalValues;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.Messages;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RoutesValidatorTest {

    private static final RoutesValidator DEFAULT_VALIDATOR = new RoutesValidator(null, false);

    static Stream<Arguments> getParameters() {
        return Stream.of(
// @formatter:off
            // [1] two routes; both are valid
            Arguments.of(List.of(routeParameter("valid-host.domain"), 
                                 routeParameter("sub.domain.com", true)), 
                         null,
                         false,
                         true,
                         List.of(routeParameter("valid-host.domain"), 
                                 routeParameter("sub.domain.com", true)), 
                         null),
            // [2] three routes; one is invalid; can be corrected
            Arguments.of(List.of(routeParameter("foo.domain.com"), 
                                 routeParameter("bar.domain.com", true), 
                                 routeParameter("baz^but$invalid.domain.com")), 
                         null,
                         false,
                         false,
                         List.of(routeParameter("foo.domain.com"), 
                                 routeParameter("bar.domain.com", true), 
                                 routeParameter("baz-but-invalid.domain.com")),
                         null),
            // [3] one route; is invalid; can be corrected
            Arguments.of(List.of(routeParameter("host.domain_can_be_corrected.com")),
                         null,
                         false,
                         false, 
                         List.of(routeParameter("host.domain-can-be-corrected.com")),
                         null),
            // [4] one hostless route; is invalid; can be corrected
            Arguments.of(List.of(routeParameter("sub_domain_can_be_corrected.domain.com", true)),
                         null,
                         false,
                         false, 
                         List.of(routeParameter("sub-domain-can-be-corrected.domain.com", true)),
                         null),
            // [5] one route containing invalid value for no-hostname flag; results in exception
            Arguments.of(List.of(routeParameterWithAdditionalValues("doesnt.matter.com", false, Map.of(NO_HOSTNAME, "not a boolean"))),
                         null,
                         false,
                         false, 
                         null,
                         MessageFormat.format(Messages.COULD_NOT_PARSE_BOOLEAN_FLAG, NO_HOSTNAME)),
            // [6] two routes, one containing a random key/value pair next to route; is ignored and valid
            Arguments.of(List.of(routeParameter("valid-route.com"), 
                                 routeParameterWithAdditionalValues("another-valid-one.com", false, Map.of("UNSUPPORTED-KEY", 1))),
                         null,
                         false,
                         false, 
                         List.of(routeParameter("valid-route.com"),
                                 routeParameter("another-valid-one.com", false)),
                         null),
            // [7] two routes where namespace prefix should be applied
            Arguments.of(List.of(routeParameter("valid-host-1.domain.com"),
                                 routeParameter("valid-host-2.domain.com")),
                         "dev",
                         true,
                         false,
                         List.of(routeParameter("dev-valid-host-1.domain.com"),
                                 routeParameter("dev-valid-host-2.domain.com")),
                         null),
            // [8] two routes where namespace prefix is already applied
            Arguments.of(List.of(routeParameter("dev-valid-host-1.domain.com"),
                                 routeParameter("dev-valid-host-2.domain.com")),
                         "dev",
                         true,
                         true,
                         List.of(routeParameter("dev-valid-host-1.domain.com"),
                                 routeParameter("dev-valid-host-2.domain.com")),
                         null),
            // [9] two routes, only one has namespace applied
            Arguments.of(List.of(routeParameter("valid-host-1.domain.com"),
                                 routeParameterWithAdditionalValues("valid-host-2.domain.com", false, Map.of(SupportedParameters.APPLY_NAMESPACE, false))),
                         "dev",
                         true,
                         false,
                         List.of(routeParameter("dev-valid-host-1.domain.com"),
                                 routeParameterWithAdditionalValues("valid-host-2.domain.com", false, Map.of(SupportedParameters.APPLY_NAMESPACE, false))),
                         null),
            // [10] two routes, only one has namespace applied; correction is already applied
            Arguments.of(List.of(routeParameter("dev-valid-host-1.domain.com"),
                                 routeParameterWithAdditionalValues("valid-host-2.domain.com", false, Map.of(SupportedParameters.APPLY_NAMESPACE, false))),
                         "dev",
                         true,
                         true,
                         List.of(routeParameter("dev-valid-host-1.domain.com"),
                                 routeParameterWithAdditionalValues("valid-host-2.domain.com", false, Map.of(SupportedParameters.APPLY_NAMESPACE, false))),
                         null),
            // [11] empty map of routes
            Arguments.of(Collections.emptyList(), null, false, false, Collections.emptyList(), org.cloudfoundry.multiapps.controller.core.Messages.COULD_NOT_PARSE_ROUTE),
            // [12] not valid route parameter
            Arguments.of(List.of("test"), null, false, false, Collections.emptyList(), org.cloudfoundry.multiapps.controller.core.Messages.COULD_NOT_PARSE_ROUTE)
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource("getParameters")
    void testValidate(List<Map<String, Object>> inputRoutes, String namespace, boolean applyNamespaceGlobal, boolean isValid,
                      List<Map<String, Object>> expectedCorrectedRoutes, String expectedException) {
        RoutesValidator validator = new RoutesValidator(namespace, applyNamespaceGlobal);
        try {
            assertEquals(isValid, validator.isValid(inputRoutes, Collections.emptyMap()));
        } catch (Exception e) {
            assertNotNull(expectedException, "Didn't expect an exception, but got " + e.getMessage());
            assertEquals(expectedException, e.getMessage(), "Exception's message doesn't match up!");
        }
    }

    @SuppressWarnings("unchecked")
    @ParameterizedTest
    @MethodSource("getParameters")
    void testAttemptToCorrect(List<Map<String, Object>> inputRoutes, String namespace, boolean applyNamespaceGlobal, boolean isValid,
                              List<Map<String, Object>> expectedCorrectedRoutes, String expectedException) {
        RoutesValidator validator = new RoutesValidator(namespace, applyNamespaceGlobal);
        try {
            List<Map<String, Object>> correctedRoutes = (List<Map<String, Object>>) validator.attemptToCorrect(inputRoutes,
                                                                                                               Collections.emptyMap());
            assertNull(expectedException, "Expected an exception but test passed!");
            assertEquals(expectedCorrectedRoutes, correctedRoutes);
        } catch (Exception e) {
            assertNotNull(expectedException, "Didn't expect an exception, but got " + e.getMessage());
            assertEquals(expectedException, e.getMessage(), "Exception's message doesn't match up!");
        }
    }

    @Test
    void testCanCorrect() {
        assertTrue(DEFAULT_VALIDATOR.canCorrect());
    }

    @Test
    void testGetParameterName() {
        assertEquals(SupportedParameters.ROUTES, DEFAULT_VALIDATOR.getParameterName());
    }

    @Test
    void testGetContainerType() {
        assertTrue(DEFAULT_VALIDATOR.getContainerType()
                                    .isAssignableFrom(Module.class));
    }

    static Stream<Arguments> testOverrideRoutesContext() {
        return Stream.of(
                         // [1] Add apply-namespace parameter from parent context when is missing in route map
                         Arguments.of(Map.of(SupportedParameters.APPLY_NAMESPACE, false),
                                      Map.of(SupportedParameters.ROUTE, "valid-host.domain.com"),
                                      Map.of(SupportedParameters.ROUTE, "valid-host.domain.com", SupportedParameters.APPLY_NAMESPACE,
                                             false)),
                         // [2] Do not override apply-namespace parameter if it is present in route map
                         Arguments.of(Map.of(SupportedParameters.APPLY_NAMESPACE, false),
                                      Map.of(SupportedParameters.ROUTE, "valid-host.domain.com", SupportedParameters.APPLY_NAMESPACE, true),
                                      Map.of(SupportedParameters.ROUTE, "dev-valid-host.domain.com", SupportedParameters.APPLY_NAMESPACE,
                                             true)),
                         // [3] Add no-hostname parameter from parent context when it is missing in route map
                         Arguments.of(Map.of(SupportedParameters.NO_HOSTNAME, true), Map.of(SupportedParameters.ROUTE, "sub.domain.com"),
                                      Map.of(SupportedParameters.ROUTE, "sub.domain.com", SupportedParameters.NO_HOSTNAME, true)));
    }

    @SuppressWarnings("unchecked")
    @ParameterizedTest
    @MethodSource
    void testOverrideRoutesContext(Map<String, Object> parentContext, Map<String, Object> inputRoute, Map<String, Object> expectedRoute) {
        RoutesValidator validator = new RoutesValidator("dev", true);
        List<Map<String, Object>> result = (List<Map<String, Object>>) validator.attemptToCorrect(List.of(inputRoute), parentContext);
        assertEquals(expectedRoute, result.get(0));
    }

}
