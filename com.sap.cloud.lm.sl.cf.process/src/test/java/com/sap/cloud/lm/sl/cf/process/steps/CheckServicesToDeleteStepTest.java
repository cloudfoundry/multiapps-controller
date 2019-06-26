package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableCloudService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

import com.sap.cloud.lm.sl.cf.core.cf.clients.EventsGetter;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceGetter;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperation;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationState;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.common.util.MapUtil;

public class CheckServicesToDeleteStepTest extends SyncFlowableStepTest<CheckServicesToDeleteStep> {

    private static final String TEST_SPACE_ID = "test";

    @Mock
    private ServiceGetter serviceInstanceGetter;
    @Mock
    private EventsGetter eventsGetter;

    public static Stream<Arguments> testExecute() {
        return Stream.of(
        //@formatter:off
            // (1) Multiple services in progress
            Arguments.of(Arrays.asList("service-1","service-2","service-3"), Arrays.asList("service-1","service-2","service-3"), 
                MapUtil.of(Pair.of("service-1", ServiceOperationState.IN_PROGRESS),Pair.of("service-2", ServiceOperationState.IN_PROGRESS),Pair.of("service-3", ServiceOperationState.IN_PROGRESS)),
                Arrays.asList("service-1","service-2","service-3"), "POLL"),
            // (2) One service in progress
            Arguments.of(Arrays.asList("service-1","service-2","service-3"), Arrays.asList("service-2","service-3"), 
                MapUtil.of(Pair.of("service-2", ServiceOperationState.SUCCEEDED),Pair.of("service-3", ServiceOperationState.IN_PROGRESS)),
                Arrays.asList("service-3"), "POLL"),
            // (3) All services are not in progress state
            Arguments.of(Arrays.asList("service-1","service-2","service-3"), Arrays.asList("service-1","service-2"), 
                MapUtil.of(Pair.of("service-1", ServiceOperationState.SUCCEEDED),Pair.of("service-2", ServiceOperationState.SUCCEEDED)),
                Collections.emptyList(), "DONE")
        //@formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testExecute(List<String> serviceNames, List<String> existingServiceNames,
        Map<String, ServiceOperationState> servicesOperationState, List<String> expectedServicesOperations, String expectedStatus) {
        prepareContext(serviceNames);
        prepareClient(existingServiceNames);
        prepareServiceInstanceGetter(servicesOperationState);

        step.execute(context);

        validateExecution(expectedServicesOperations, expectedStatus);
    }

    private void prepareContext(List<String> serviceNames) {
        StepsUtil.setSpaceId(context, TEST_SPACE_ID);
        StepsUtil.setServicesToDelete(context, serviceNames);
    }

    private void prepareClient(List<String> existingServiceNames) {
        for (String serviceName : existingServiceNames) {
            when(client.getService(serviceName, false)).thenReturn(ImmutableCloudService.builder()
                .name(serviceName)
                .metadata(ImmutableCloudMetadata.builder()
                    .guid(UUID.randomUUID())
                    .build())
                .build());
        }
    }

    private void prepareServiceInstanceGetter(Map<String, ServiceOperationState> servicesOperationState) {
        for (String serviceName : servicesOperationState.keySet()) {
            Map<String, Object> serviceOperationMap = new HashMap<>();
            serviceOperationMap.put(ServiceOperation.SERVICE_OPERATION_TYPE, ServiceOperationType.UPDATE);
            serviceOperationMap.put(ServiceOperation.SERVICE_OPERATION_STATE, servicesOperationState.get(serviceName));
            when(serviceInstanceGetter.getServiceInstanceEntity(client, serviceName, TEST_SPACE_ID))
                .thenReturn(MapUtil.asMap(ServiceOperation.LAST_SERVICE_OPERATION, serviceOperationMap));
        }
    }

    private void validateExecution(List<String> expectedServicesOperations, String expectedStatus) {
        Map<String, ServiceOperationType> triggeredServiceOperations = StepsUtil.getTriggeredServiceOperations(context);
        for (String serviceName : expectedServicesOperations) {
            ServiceOperationType serviceOperationType = MapUtils.getObject(triggeredServiceOperations, serviceName);
            assertNotNull(serviceOperationType);
        }
        assertEquals(expectedStatus, getExecutionStatus());
    }

    @Override
    protected CheckServicesToDeleteStep createStep() {
        return new CheckServicesToDeleteStep();
    }

}
