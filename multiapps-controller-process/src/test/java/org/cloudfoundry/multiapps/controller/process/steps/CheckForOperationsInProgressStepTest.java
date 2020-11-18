package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.collections4.MapUtils;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.util.ServiceOperationGetter;
import org.cloudfoundry.multiapps.controller.process.util.ServiceProgressReporter;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ServiceOperation;

class CheckForOperationsInProgressStepTest extends SyncFlowableStepTest<CheckForOperationsInProgressStep> {

    private static final String TEST_SPACE_ID = "test";

    static Stream<Arguments> testExecute() {
        return Stream.of(
                         // (1) Service exist and it is in progress state
                         Arguments.of("service-1", true,
                                      new ServiceOperation(ServiceOperation.Type.CREATE, "", ServiceOperation.State.IN_PROGRESS),
                                      ServiceOperation.Type.CREATE, "POLL"),
                         // (2) Service does not exist
                         Arguments.of("service-1", false, null, null, "DONE"),
                         // (3) Service exist but it is not in progress state
                         Arguments.of("service-1", true,
                                      new ServiceOperation(ServiceOperation.Type.CREATE, "", ServiceOperation.State.SUCCEEDED), null,
                                      "DONE"),
                         // (4) Missing service operation for existing service
                         Arguments.of("service-1", true, null, null, "DONE"),
                         // (5) Missing type and state for last operation
                         Arguments.of("service-1", true, new ServiceOperation(null, null, null), null, "DONE"));
    }

    @ParameterizedTest
    @MethodSource
    void testExecute(String serviceName, boolean serviceExist, ServiceOperation serviceOperation,
                     ServiceOperation.Type expectedTriggeredServiceOperation, String expectedStatus) {
        CloudServiceInstanceExtended service = ImmutableCloudServiceInstanceExtended.builder()
                                                                                    .name(serviceName)
                                                                                    .metadata(ImmutableCloudMetadata.builder()
                                                                                                                    .guid(UUID.randomUUID())
                                                                                                                    .build())
                                                                                    .build();
        prepareContext(service);
        prepareClient(service, serviceOperation, serviceExist);

        step.execute(execution);
        validateExecution(serviceName, expectedTriggeredServiceOperation, expectedStatus);
    }

    private void prepareClient(CloudServiceInstanceExtended service, ServiceOperation serviceOperation, boolean serviceExist) {
        if (serviceExist) {
            CloudServiceInstanceExtended returnedService = ImmutableCloudServiceInstanceExtended.builder()
                                                                                                .from(service)
                                                                                                .lastOperation(serviceOperation)
                                                                                                .build();
            when(client.getServiceInstance(service.getName(), false)).thenReturn(returnedService);
        }
    }

    protected void prepareContext(CloudServiceInstanceExtended service) {
        context.setVariable(Variables.SPACE_GUID, TEST_SPACE_ID);
        context.setVariable(Variables.SERVICE_TO_PROCESS, service);
    }

    private void validateExecution(String serviceName, ServiceOperation.Type expectedTriggeredServiceOperation, String expectedStatus) {
        Map<String, ServiceOperation.Type> triggeredServiceOperations = context.getVariable(Variables.TRIGGERED_SERVICE_OPERATIONS);
        ServiceOperation.Type serviceOperationType = MapUtils.getObject(triggeredServiceOperations, serviceName);
        assertEquals(serviceOperationType, expectedTriggeredServiceOperation);
        assertEquals(expectedStatus, getExecutionStatus());
    }

    @Override
    protected CheckForOperationsInProgressStep createStep() {
        return new CheckForOperationsInProgressStep();
    }

}
