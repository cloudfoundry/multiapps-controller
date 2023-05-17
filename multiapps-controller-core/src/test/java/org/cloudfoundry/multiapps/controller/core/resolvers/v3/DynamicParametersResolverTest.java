package org.cloudfoundry.multiapps.controller.core.resolvers.v3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.core.helpers.DynamicResolvableParametersHelper;
import org.cloudfoundry.multiapps.controller.core.model.DynamicResolvableParameter;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDynamicResolvableParameter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DynamicParametersResolverTest {

    private static final String SERVICE_RESOURCE_NAME = "test-resource";

    static Stream<Arguments> testResolveDynamicParameter() {
        return Stream.of(
                         // (1) Test replace template with service guid
                         Arguments.of("{ds/service-1/service-guid}", Set.of(ImmutableDynamicResolvableParameter.builder()
                                                                                                               .parameterName("service-guid")
                                                                                                               .relationshipEntityName("service-1")
                                                                                                               .value("1")
                                                                                                               .build(),
                                                                            ImmutableDynamicResolvableParameter.builder()
                                                                                                               .parameterName("service-guid")
                                                                                                               .relationshipEntityName("service-2")
                                                                                                               .value("2")
                                                                                                               .build(),
                                                                            ImmutableDynamicResolvableParameter.builder()
                                                                                                               .parameterName("metaspace-key")
                                                                                                               .relationshipEntityName("service-1")
                                                                                                               .value("new-value")
                                                                                                               .build()),
                                      "1", false),
                         // (2) Test throw exception due to not resolved service guid value
                         Arguments.of("{ds/service-1/service-guid}", Set.of(ImmutableDynamicResolvableParameter.builder()
                                                                                                               .parameterName("service-guid")
                                                                                                               .relationshipEntityName("service-1")
                                                                                                               .build()),
                                      null, true),
                         // (3) Test skip replacement of parameter value due to not matching of template
                         Arguments.of("test-parameter-value", Set.of(ImmutableDynamicResolvableParameter.builder()
                                                                                                        .parameterName("service-guid")
                                                                                                        .relationshipEntityName("service-1")
                                                                                                        .build()),
                                      "test-parameter-value", false));
    }

    @ParameterizedTest
    @MethodSource
    void testResolveDynamicParameter(String descriptorParameterValue, Set<DynamicResolvableParameter> dynamicResolvableParameters,
                                     String expectedValue, boolean expectedException) {
        DynamicParametersResolver resolver = new DynamicParametersResolver(SERVICE_RESOURCE_NAME,
                                                                           new DynamicResolvableParametersHelper(dynamicResolvableParameters));

        if (expectedException) {
            assertThrows(ContentException.class, () -> resolver.visit(null, descriptorParameterValue));
            return;
        }

        Object resolvedValue = resolver.visit(null, descriptorParameterValue);
        assertEquals(expectedValue, resolvedValue);
    }

}
