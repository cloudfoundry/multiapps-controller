package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceInstance;
import com.sap.cloudfoundry.client.facade.domain.ServiceOperation;

class CheckServicesToDeleteStepTest extends SyncFlowableStepTest<CheckServicesToDeleteStep> {

    private static final String TEST_SPACE_ID = "test";

    @Mock
    private ApplicationConfiguration applicationConfiguration;

    static Stream<Arguments> testExecute() {
        return Stream.of(
                         // (1) Multiple services in progress
                         Arguments.of(List.of("service-1", "service-2", "service-3"), List.of("service-1", "service-2", "service-3"),
                                      Map.ofEntries(Map.entry("service-1", ServiceOperation.State.IN_PROGRESS),
                                                    Map.entry("service-2", ServiceOperation.State.IN_PROGRESS),
                                                    Map.entry("service-3", ServiceOperation.State.IN_PROGRESS)),
                                      List.of("service-1", "service-2", "service-3"), "POLL"),
                         // (2) One service in progress
                         Arguments.of(List.of("service-1", "service-2", "service-3"), List.of("service-2", "service-3"),
                                      Map.ofEntries(Pair.of("service-2", ServiceOperation.State.SUCCEEDED),
                                                    Pair.of("service-3", ServiceOperation.State.IN_PROGRESS)),
                                      List.of("service-3"), "POLL"),
                         // (3) All services are not in progress state
                         Arguments.of(List.of("service-1", "service-2", "service-3"), List.of("service-1", "service-2"),
                                      Map.ofEntries(Pair.of("service-1", ServiceOperation.State.SUCCEEDED),
                                                    Pair.of("service-2", ServiceOperation.State.SUCCEEDED)),
                                      Collections.emptyList(), "DONE"));
    }

    @ParameterizedTest
    @MethodSource
    void testExecute(List<String> serviceNames, List<String> existingServiceNames,
                     Map<String, ServiceOperation.State> servicesOperationState, List<String> expectedServicesOperations,
                     String expectedStatus) {
        prepareContext(serviceNames);
        prepareClient(existingServiceNames, servicesOperationState);
        prepareApplicationConfiguration();

        step.execute(execution);

        validateExecution(expectedServicesOperations, expectedStatus);
    }

    private void prepareContext(List<String> serviceNames) {
        context.setVariable(Variables.SPACE_GUID, TEST_SPACE_ID);
        context.setVariable(Variables.SERVICES_TO_DELETE, serviceNames);
    }

    private List<CloudServiceInstance> getServices(List<String> existingServiceNames) {
        return existingServiceNames.stream()
                                   .map(serviceName -> ImmutableCloudServiceInstance.builder()
                                                                                    .name(serviceName)
                                                                                    .metadata(ImmutableCloudMetadata.builder()
                                                                                                                    .guid(UUID.randomUUID())
                                                                                                                    .build())
                                                                                    .build())
                                   .collect(Collectors.toList());

    }

    private void prepareClient(List<String> serviceNames, Map<String, ServiceOperation.State> servicesOperationState) {
        for (String serviceName: serviceNames) {
            ServiceOperation lastServiceOperation = new ServiceOperation(ServiceOperation.Type.DELETE,
                                                                         "",
                                                                         servicesOperationState.get(serviceName));
            CloudServiceInstanceExtended returnedService = ImmutableCloudServiceInstanceExtended.builder()
                                                                                                .metadata(ImmutableCloudMetadata.builder()
                                                                                                                                .guid(UUID.randomUUID())
                                                                                                                                .build())
                                                                                                .name(serviceName)
                                                                                                .lastOperation(lastServiceOperation)
                                                                                                .build();
            when(client.getServiceInstance(serviceName, false)).thenReturn(returnedService);
        }
    }

    private void prepareApplicationConfiguration() {
        when(applicationConfiguration.getServiceHandlingMaxParallelThreads()).thenReturn(ApplicationConfiguration.DEFAULT_SERVICE_HANDLING_MAX_PARALLEL_THREADS);
    }

    private void validateExecution(List<String> expectedServicesOperations, String expectedStatus) {
        Map<String, ServiceOperation.Type> triggeredServiceOperations = context.getVariable(Variables.TRIGGERED_SERVICE_OPERATIONS);
        for (String serviceName : expectedServicesOperations) {
            ServiceOperation.Type serviceOperationType = MapUtils.getObject(triggeredServiceOperations, serviceName);
            assertNotNull(serviceOperationType);
        }
        assertEquals(expectedStatus, getExecutionStatus());
    }

    @Override
    protected CheckServicesToDeleteStep createStep() {
        return new CheckServicesToDeleteStep();
    }
}
