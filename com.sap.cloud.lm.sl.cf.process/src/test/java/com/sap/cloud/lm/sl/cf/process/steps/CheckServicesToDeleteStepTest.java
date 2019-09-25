package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableCloudService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperation;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationState;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.process.util.ServiceOperationGetter;
import com.sap.cloud.lm.sl.common.util.MapUtil;

public class CheckServicesToDeleteStepTest extends SyncFlowableStepTest<CheckServicesToDeleteStep> {

    private static final String TEST_SPACE_ID = "test";

    @Mock
    private ServiceOperationGetter serviceOperationGetter;

    @SuppressWarnings("unchecked")
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
                            Map<String, ServiceOperationState> servicesOperationState, List<String> expectedServicesOperations,
                            String expectedStatus) {
        prepareContext(serviceNames);
        List<CloudService> services = getServices(existingServiceNames);
        prepareClient(services);
        prepareServiceOperationGetter(servicesOperationState);

        step.execute(context);

        validateExecution(expectedServicesOperations, expectedStatus);
    }

    private void prepareContext(List<String> serviceNames) {
        StepsUtil.setSpaceId(context, TEST_SPACE_ID);
        StepsUtil.setServicesToDelete(context, serviceNames);
    }

    private List<CloudService> getServices(List<String> existingServiceNames) {
        return existingServiceNames.stream()
                                   .map(serviceName -> ImmutableCloudService.builder()
                                                                            .name(serviceName)
                                                                            .metadata(ImmutableCloudMetadata.builder()
                                                                                                            .guid(UUID.randomUUID())
                                                                                                            .build())
                                                                            .build())
                                   .collect(Collectors.toList());

    }

    private void prepareClient(List<CloudService> services) {
        for (CloudService service : services) {
            when(client.getService(service.getName(), false)).thenReturn(service);
        }
    }

    private void prepareServiceOperationGetter(Map<String, ServiceOperationState> servicesOperationState) {
        for (String serviceName : servicesOperationState.keySet()) {
            ServiceOperation serviceOperation = new ServiceOperation(ServiceOperationType.DELETE,
                                                                     "",
                                                                     servicesOperationState.get(serviceName));
            when(serviceOperationGetter.getLastServiceOperation(any(),
                                                                argThat(new CloudServiceExtendedMatcher(serviceName)))).thenReturn(serviceOperation);
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

    private class CloudServiceExtendedMatcher implements ArgumentMatcher<CloudServiceExtended> {

        private String serviceName;

        public CloudServiceExtendedMatcher(String serviceName) {
            this.serviceName = serviceName;
        }

        @Override
        public boolean matches(CloudServiceExtended service) {
            return service != null && service.getName()
                                             .equals(serviceName);
        }

    }

}
