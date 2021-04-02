package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.util.ServiceOperationGetter;
import org.cloudfoundry.multiapps.controller.process.util.ServiceProgressReporter;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ServiceOperation;

class PollServiceInProgressOperationsExecutionTest extends AsyncStepOperationTest<CheckServiceOperationStateStep> {

    private static final String TEST_SPACE_ID = "test";
    private static final String TEST_PROVIDER = "testProvider";
    private static final String TEST_PLAN = "testPlan";
    private static final String TEST_VERSION = "0.0.1-beta";

    @Mock
    private ServiceOperationGetter serviceOperationGetter;
    @Mock
    private ServiceProgressReporter serviceProgressReporter;
    @Mock
    private CloudControllerClient client;

    private boolean shouldVerifyStepLogger;
    private AsyncExecutionState expectedExecutionState;

    public static Stream<Arguments> testPollStateExecution() {
        return Stream.of(
// @formatter:off
            // (0) With 2 services in progress:
            Arguments.of(List.of("service1","service2", "service3"),
                    List.of(ServiceOperation.Type.DELETE, ServiceOperation.Type.CREATE, ServiceOperation.Type.DELETE),
                    List.of(ServiceOperation.State.IN_PROGRESS, ServiceOperation.State.SUCCEEDED, ServiceOperation.State.IN_PROGRESS),
                    false, AsyncExecutionState.RUNNING, null),
            // (1) With 1 service in progress state and 1 successfully deleted
            Arguments.of(List.of("service1","service2", "service3"),
                    List.of(ServiceOperation.Type.DELETE, ServiceOperation.Type.CREATE, ServiceOperation.Type.DELETE),
                    Arrays.asList(null, ServiceOperation.State.SUCCEEDED, ServiceOperation.State.IN_PROGRESS),
                    true, AsyncExecutionState.RUNNING, null),
            // (2) With 3 services finished operations successfully 
            Arguments.of(List.of("service1","service2", "service3"),
                    List.of(ServiceOperation.Type.UPDATE, ServiceOperation.Type.CREATE, ServiceOperation.Type.DELETE),
                    List.of(ServiceOperation.State.SUCCEEDED, ServiceOperation.State.SUCCEEDED, ServiceOperation.State.SUCCEEDED),
                    false, AsyncExecutionState.FINISHED, null),
            // (3) Handle missing response for last service operation
            Arguments.of(List.of("service1","service2", "service3"),
                    Arrays.asList(null, ServiceOperation.Type.CREATE, null),
                    Arrays.asList(null, null, null),
                    false, AsyncExecutionState.FINISHED, null),
            // (4) Throw exception on create failed service state
            Arguments.of(List.of("service1","service2", "service3"),
                    Arrays.asList(null, ServiceOperation.Type.CREATE, null),
                    Arrays.asList(null, ServiceOperation.State.FAILED, null),
                    true, null, "Error creating service \"service2\" from offering \"null\" and plan \"testPlan\""),
            // (5) Throw exception on update failed service state
            Arguments.of(List.of("service1","service2", "service3"),
                    Arrays.asList(null, ServiceOperation.Type.UPDATE, null),
                    Arrays.asList(null, ServiceOperation.State.FAILED, null),
                    true, null, "Error updating service \"service2\" from offering \"null\" and plan \"testPlan\""),
            // (5) Throw exception on delete failed service state
            Arguments.of(List.of("service1","service2", "service3"),
                    Arrays.asList(null, ServiceOperation.Type.DELETE, null),
                    Arrays.asList(null, ServiceOperation.State.FAILED, null),
                    true, null, "Error deleting service \"service2\" from offering \"null\" and plan \"testPlan\"")     
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testPollStateExecution(List<String> serviceNames, List<ServiceOperation.Type> servicesOperationTypes,
                                List<ServiceOperation.State> servicesOperationStates, boolean shouldVerifyStepLogger,
                                AsyncExecutionState expectedExecutionState, String expectedExceptionMessage) {
        this.shouldVerifyStepLogger = shouldVerifyStepLogger;
        this.expectedExecutionState = expectedExecutionState;
        initializeParameters(serviceNames, servicesOperationTypes, servicesOperationStates);
        if (expectedExceptionMessage != null) {
            Exception exception = assertThrows(Exception.class, this::testExecuteOperations);
            assertTrue(exception.getMessage()
                                .contains(expectedExceptionMessage));
            return;
        }
        testExecuteOperations();
    }

    private void initializeParameters(List<String> serviceNames, List<ServiceOperation.Type> servicesOperationTypes,
                                      List<ServiceOperation.State> servicesOperationStates) {
        context.setVariable(Variables.SPACE_GUID, TEST_SPACE_ID);
        List<CloudServiceInstanceExtended> services = generateCloudServicesExtended(serviceNames);
        prepareServiceOperationGetter(services, servicesOperationTypes, servicesOperationStates);
        prepareServicesData(services);
        prepareTriggeredServiceOperations(serviceNames, servicesOperationTypes);
        when(clientProvider.getControllerClient(anyString(), anyString(), anyString())).thenReturn(client);

    }

    private void prepareServiceOperationGetter(List<CloudServiceInstanceExtended> services,
                                               List<ServiceOperation.Type> servicesOperationTypes,
                                               List<ServiceOperation.State> servicesOperationStates) {
        for (int i = 0; i < services.size(); i++) {
            CloudServiceInstanceExtended service = services.get(i);
            ServiceOperation.Type serviceOperationType = servicesOperationTypes.get(i);
            ServiceOperation.State serviceOperationState = servicesOperationStates.get(i);
            if (serviceOperationType != null && serviceOperationState != null) {
                when(serviceOperationGetter.getLastServiceOperation(any(),
                                                                    eq(service))).thenReturn(new ServiceOperation(serviceOperationType,
                                                                                                                  "",
                                                                                                                  serviceOperationState));
            }
        }
    }

    private void prepareTriggeredServiceOperations(List<String> serviceNames, List<ServiceOperation.Type> servicesOperationTypes) {
        Map<String, ServiceOperation.Type> triggeredServiceOperations = new HashMap<>();
        for (int index = 0; index < serviceNames.size(); index++) {
            String serviceName = serviceNames.get(index);
            ServiceOperation.Type serviceOperationType = servicesOperationTypes.get(index);
            if (serviceOperationType != null) {
                triggeredServiceOperations.put(serviceName, serviceOperationType);
            }
        }
        context.setVariable(Variables.TRIGGERED_SERVICE_OPERATIONS, triggeredServiceOperations);
    }

    private List<CloudServiceInstanceExtended> generateCloudServicesExtended(List<String> serviceNames) {
        return serviceNames.stream()
                           .map(this::buildCloudServiceExtended)
                           .collect(Collectors.toList());
    }

    private ImmutableCloudServiceInstanceExtended buildCloudServiceExtended(String serviceName) {
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .name(serviceName)
                                                    .provider(TEST_PROVIDER)
                                                    .plan(TEST_PLAN)
                                                    .version(TEST_VERSION)
                                                    .metadata(ImmutableCloudMetadata.builder()
                                                                                    .guid(UUID.randomUUID())
                                                                                    .build())
                                                    .build();
    }

    private void prepareServicesData(List<CloudServiceInstanceExtended> services) {
        context.setVariable(Variables.SERVICES_DATA, services);
    }

    @Override
    protected CheckServiceOperationStateStep createStep() {
        return new CheckServiceOperationStateStep(serviceOperationGetter, serviceProgressReporter);
    }

    @Override
    protected void validateOperationExecutionResult(AsyncExecutionState result) {
        if (shouldVerifyStepLogger) {
            verify(stepLogger).warnWithoutProgressMessage(anyString(), any());
        }
        assertEquals(expectedExecutionState, result);
    }

    @Override
    protected List<AsyncExecution> getAsyncOperations(ProcessContext wrapper) {
        return step.getAsyncStepExecutions(wrapper);
    }
}
