package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.collections4.MapUtils;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableCloudService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.EventsGetter;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceGetter;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperation;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationState;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.common.util.MapUtil;

public class CheckForOperationsInProgressStepTest extends SyncFlowableStepTest<CheckForOperationsInProgressStep> {

    private static final String TEST_SPACE_ID = "test";

    @Mock
    private ServiceGetter serviceInstanceGetter;
    @Mock
    private EventsGetter eventsGetter;

    public static Stream<Arguments> testExecute() {
        return Stream.of(
        //@formatter:off
            // (1) Service exist and it is in progress state
            Arguments.of("service-1", true, new ServiceOperation(ServiceOperationType.CREATE, "", ServiceOperationState.IN_PROGRESS), ServiceOperationType.CREATE, "POLL"),
            // (2) Service does not exist
            Arguments.of("service-1", false, null, null, "DONE"),
            // (3) Service exist but it is not in progress state
            Arguments.of("service-1", true, new ServiceOperation(ServiceOperationType.CREATE, "", ServiceOperationState.SUCCEEDED), null, "DONE"),
            // (4) Missing service operation for existing service
            Arguments.of("service-1", true, null, null, "DONE"),
            // (5) Missing type and state for last operation
            Arguments.of("service-1", true, new ServiceOperation(null, null, null), null, "DONE")
        //@formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testExecute(String serviceName, boolean serviceExist, ServiceOperation serviceOperation,
        ServiceOperationType expectedTriggeredServiceOperation, String expectedStatus) {
        prepareContext(serviceName);
        prepareClient(serviceName, serviceExist);
        prepareServiceInstanceGetter(serviceName, serviceOperation);

        step.execute(context);
        validateExecution(serviceName, expectedTriggeredServiceOperation, expectedStatus);
    }

    private void prepareClient(String serviceName, boolean serviceExist) {
        if (serviceExist) {
            when(client.getService(serviceName, false)).thenReturn(ImmutableCloudService.builder()
                .name(serviceName)
                .metadata(ImmutableCloudMetadata.builder()
                    .guid(UUID.randomUUID())
                    .build())
                .build());
        }
    }

    protected void prepareContext(String service) {
        StepsUtil.setSpaceId(context, TEST_SPACE_ID);
        StepsUtil.setServiceToProcess(ImmutableCloudServiceExtended.builder()
            .name(service)
            .build(), context);
    }

    private void prepareServiceInstanceGetter(String serviceName, ServiceOperation serviceOperation) {
        if (serviceOperation != null) {
            Map<String, Object> serviceOperationMap = new HashMap<>();
            serviceOperationMap.put(ServiceOperation.SERVICE_OPERATION_TYPE, serviceOperation.getType());
            serviceOperationMap.put(ServiceOperation.SERVICE_OPERATION_STATE, serviceOperation.getState());
            when(serviceInstanceGetter.getServiceInstanceEntity(client, serviceName, TEST_SPACE_ID))
                .thenReturn(MapUtil.asMap(ServiceOperation.LAST_SERVICE_OPERATION, serviceOperationMap));
        }
    }

    private void validateExecution(String serviceName, ServiceOperationType expectedTriggeredServiceOperation, String expectedStatus) {
        Map<String, ServiceOperationType> triggeredServiceOperations = StepsUtil.getTriggeredServiceOperations(context);
        ServiceOperationType serviceOperationType = MapUtils.getObject(triggeredServiceOperations, serviceName);
        assertEquals(serviceOperationType, expectedTriggeredServiceOperation);
        assertEquals(expectedStatus, getExecutionStatus());
    }

    @Override
    protected CheckForOperationsInProgressStep createStep() {
        return new CheckForOperationsInProgressStep();
    }

}
