package org.cloudfoundry.multiapps.controller.core.helpers.v3;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cloudfoundry.multiapps.controller.core.model.DynamicResolvableParameter;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDynamicResolvableParameter;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.junit.jupiter.api.Test;

class DynamicResolvableParametersFactoryTest {

    @Test
    void testDetectionDynamicParameters() {
        Set<DynamicResolvableParameter> expectedParameters = Set.of(ImmutableDynamicResolvableParameter.builder()
                                                                                                       .parameterName("service-guid")
                                                                                                       .relationshipEntityName("test-resource-1")
                                                                                                       .build(),
                                                                    ImmutableDynamicResolvableParameter.builder()
                                                                                                       .parameterName("service-guid")
                                                                                                       .relationshipEntityName("test-resource-2")
                                                                                                       .build());

        DeploymentDescriptor descriptor = DeploymentDescriptor.createV3()
                                                              .setId("test-id")
                                                              .setResources(List.of(Resource.createV3()
                                                                                            .setName("test-resource-1")
                                                                                            .setParameters(Map.of("my-db-guid",
                                                                                                                  "{ds/test-resource-1/service-guid}")),
                                                                                    Resource.createV3()
                                                                                            .setName("test-resource-2")
                                                                                            .setProperties(Map.of("db-service-guid",
                                                                                                                  "{ds/test-resource-2/service-guid}"))));

        DynamicResolvableParametersFactory factory = new DynamicResolvableParametersFactory(descriptor);
        Set<DynamicResolvableParameter> resultParameters = factory.create();

        assertTrue(expectedParameters.containsAll(resultParameters),
                   MessageFormat.format("Expected values \"{0}\" were not found in the Set \"{1}\"", expectedParameters, resultParameters));
    }

    @Test
    void testParametersResolveDepthIsAlwaysOne() {
        DeploymentDescriptor descriptor = DeploymentDescriptor.createV3()
                                                              .setId("test-id")
                                                              .setResources(List.of(Resource.createV3()
                                                                                            .setName("test-resource-1")
                                                                                            .setParameters(Map.of("my-custom-map",
                                                                                                                  Map.of("my-db-guid",
                                                                                                                         "{ds/test-resource-1/service-guid}")))));

        DynamicResolvableParametersFactory factory = new DynamicResolvableParametersFactory(descriptor);
        Set<DynamicResolvableParameter> resultParameters = factory.create();

        assertTrue(resultParameters.isEmpty(), MessageFormat.format("Expected empty Set but contains \"{0}\"", resultParameters));
    }

}
