package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.client.v3.serviceinstances.ServiceInstanceType;
import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.cf.v2.ResourceType;
import org.cloudfoundry.multiapps.controller.core.model.DynamicResolvableParameter;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDynamicResolvableParameter;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ExtractBatchedServicesWithResolvedDynamicParametersStepTest
    extends SyncFlowableStepTest<ExtractBatchedServicesWithResolvedDynamicParametersStep> {

    private static final String SERVICE_NAME_1 = "service-1";
    private static final String SERVICE_NAME_2 = "service-2";
    private static final String SERVICE_NAME_3 = "service-3";

    static Stream<Arguments> testExecute() {
        return Stream.of(
                         // (1) 3 input services but only 2 will be created due to specified "existing-service" resource type
                         Arguments.of(List.of(ImmutableCloudServiceInstanceExtended.builder()
                                                                                   .name(SERVICE_NAME_1)
                                                                                   .type(ServiceInstanceType.MANAGED)
                                                                                   .isManaged(true)
                                                                                   .build(),
                                              ImmutableCloudServiceInstanceExtended.builder()
                                                                                   .name(SERVICE_NAME_2)
                                                                                   .type(ServiceInstanceType.USER_PROVIDED)
                                                                                   .isManaged(true)
                                                                                   .build(),
                                              ImmutableCloudServiceInstanceExtended.builder()
                                                                                   .name(SERVICE_NAME_3)
                                                                                   .isManaged(false)
                                                                                   .build()),
                                      Collections.emptySet(), List.of(ImmutableCloudServiceInstanceExtended.builder()
                                                                                                           .resourceName(SERVICE_NAME_1)
                                                                                                           .type(ServiceInstanceType.MANAGED)
                                                                                                           .build(),
                                                                      ImmutableCloudServiceInstanceExtended.builder()
                                                                                                           .resourceName(SERVICE_NAME_2)
                                                                                                           .type(ServiceInstanceType.USER_PROVIDED)
                                                                                                           .build()),
                                      false),
                         // (2) Resolve dynamic parameter inside parameters
                         Arguments.of(List.of(ImmutableCloudServiceInstanceExtended.builder()
                                                                                   .name(SERVICE_NAME_2)
                                                                                   .type(ServiceInstanceType.USER_PROVIDED)
                                                                                   .isManaged(true)
                                                                                   .credentials(Map.of("db-service-guid",
                                                                                                       "{ds/service-1/service-guid}"))
                                                                                   .build()),
                                      Set.of(ImmutableDynamicResolvableParameter.builder()
                                                                                .relationshipEntityName(SERVICE_NAME_1)
                                                                                .parameterName("service-guid")
                                                                                .value("1")
                                                                                .build()),
                                      List.of(ImmutableCloudServiceInstanceExtended.builder()
                                                                                   .resourceName(SERVICE_NAME_2)
                                                                                   .type(ServiceInstanceType.USER_PROVIDED)
                                                                                   .credentials(Map.of("db-service-guid", "1"))
                                                                                   .build()),
                                      false),
                         // (3) Fail step due to not resolved dynamic parameter
                         Arguments.of(List.of(ImmutableCloudServiceInstanceExtended.builder()
                                                                                   .name(SERVICE_NAME_2)
                                                                                   .type(ServiceInstanceType.USER_PROVIDED)
                                                                                   .credentials(Map.of("db-service-guid",
                                                                                                       "{ds/service-1/service-guid}"))
                                                                                   .build()),
                                      Set.of(ImmutableDynamicResolvableParameter.builder()
                                                                                .relationshipEntityName(SERVICE_NAME_1)
                                                                                .parameterName("service-guid")
                                                                                .build()),
                                      null, true)

        );
    }

    @ParameterizedTest
    @MethodSource
    void testExecute(List<CloudServiceInstanceExtended> batchToProcess, Set<DynamicResolvableParameter> dynamicResolvableParameters,
                     List<CloudServiceInstanceExtended> expectedServicesToCreate, boolean expectedException) {
        loadParameters(batchToProcess, dynamicResolvableParameters);

        if (expectedException) {
            assertThrows(ContentException.class, () -> step.execute(execution));
            return;
        }
        step.execute(execution);
        assertStepFinishedSuccessfully();

        List<String> expectedServiceNames = expectedServicesToCreate.stream()
                                                                    .map(CloudServiceInstanceExtended::getResourceName)
                                                                    .collect(Collectors.toList());
        Map<String, ServiceInstanceType> expectedServicesType = expectedServicesToCreate.stream()
                                                                                        .collect(Collectors.toMap(CloudServiceInstanceExtended::getResourceName,
                                                                                                                  CloudServiceInstanceExtended::getType));
        Map<String, Map<String, Object>> expectedServicesParameters = expectedServicesToCreate.stream()
                                                                                              .collect(Collectors.toMap(CloudServiceInstanceExtended::getResourceName,
                                                                                                                        CloudServiceInstanceExtended::getCredentials));

        List<CloudServiceInstanceExtended> servicesToCreateResult = context.getVariable(Variables.SERVICES_TO_CREATE);
        for (var serviceToCreateResult : servicesToCreateResult) {
            assertTrue(expectedServiceNames.contains(serviceToCreateResult.getName()),
                       MessageFormat.format("Service instance \"{0}\" is not expected to be created", serviceToCreateResult.getName()));
            assertEquals(expectedServicesType.get(serviceToCreateResult.getName()), serviceToCreateResult.getType());
            assertEquals(expectedServicesParameters.get(serviceToCreateResult.getName()), serviceToCreateResult.getCredentials());
        }
        assertEquals(expectedServicesToCreate.size(), context.getVariable(Variables.SERVICES_TO_CREATE_COUNT));
    }

    @Test
    void testResolveServiceGuidOfExistingService() {
        loadParameters(List.of(ImmutableCloudServiceInstanceExtended.builder()
                                                                    .resourceName(SERVICE_NAME_3)
                                                                    .isManaged(false)
                                                                    .build()),
                       Set.of(ImmutableDynamicResolvableParameter.builder()
                                                                 .relationshipEntityName(SERVICE_NAME_3)
                                                                 .parameterName("service-guid")
                                                                 .build()));

        UUID serviceGuid = UUID.randomUUID();
        when(client.getRequiredServiceInstanceGuid(any())).thenReturn(serviceGuid);

        DynamicResolvableParameter expectedDynamicParameter = ImmutableDynamicResolvableParameter.builder()
                                                                                                 .relationshipEntityName(SERVICE_NAME_3)
                                                                                                 .parameterName("service-guid")
                                                                                                 .value(serviceGuid.toString())
                                                                                                 .build();

        step.execute(execution);
        assertStepFinishedSuccessfully();
        assertTrue(context.getVariable(Variables.DYNAMIC_RESOLVABLE_PARAMETERS)
                          .contains(expectedDynamicParameter));
    }

    private void loadParameters(List<CloudServiceInstanceExtended> batchToProcess,
                                Set<DynamicResolvableParameter> dynamicResolvableParameters) {
        context.setVariable(Variables.BATCH_TO_PROCESS, batchToProcess);
        context.setVariable(Variables.DYNAMIC_RESOLVABLE_PARAMETERS, dynamicResolvableParameters);
        context.setVariable(Variables.MTA_MAJOR_SCHEMA_VERSION, 3);
        context.setVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR, getDeploymentDescriptor());
    }

    private DeploymentDescriptor getDeploymentDescriptor() {
        return DeploymentDescriptor.createV3()
                                   .setResources(List.of(Resource.createV3()
                                                                 .setName(SERVICE_NAME_1)
                                                                 .setType(ResourceType.MANAGED_SERVICE.toString()),
                                                         Resource.createV3()
                                                                 .setName(SERVICE_NAME_2)
                                                                 .setType(ResourceType.USER_PROVIDED_SERVICE.toString()),
                                                         Resource.createV3()
                                                                 .setName(SERVICE_NAME_3)
                                                                 .setType(ResourceType.EXISTING_SERVICE.toString())));
    }

    @Override
    protected ExtractBatchedServicesWithResolvedDynamicParametersStep createStep() {
        return new ExtractBatchedServicesWithResolvedDynamicParametersStep();
    }

}