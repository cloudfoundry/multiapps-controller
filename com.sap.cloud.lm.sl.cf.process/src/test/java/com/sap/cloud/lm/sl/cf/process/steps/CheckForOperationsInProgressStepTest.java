package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.collections4.MapUtils;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperation;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationState;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.process.util.ServiceOperationGetter;
import com.sap.cloud.lm.sl.cf.process.util.ServiceProgressReporter;

public class CheckForOperationsInProgressStepTest extends SyncFlowableStepTest<CheckForOperationsInProgressStep> {

    private static final String TEST_SPACE_ID = "test";

    @Mock
    private ServiceOperationGetter serviceOperationGetter;
    @Mock
    private ServiceProgressReporter serviceProgressReporter;

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
                            ServiceOperationType expectedTriggeredServiceOperation, String expectedStatus)
        throws Exception {
        CloudServiceExtended service = ImmutableCloudServiceExtended.builder()
                                                                    .name(serviceName)
                                                                    .metadata(ImmutableCloudMetadata.builder()
                                                                                                    .guid(UUID.randomUUID())
                                                                                                    .build())
                                                                    .build();
        prepareContext(service);
        prepareClient(service, serviceExist);
        prepareServiceInstanceGetter(service, serviceOperation);

        step.execute(context);
        validateExecution(serviceName, expectedTriggeredServiceOperation, expectedStatus);
    }

    private void prepareClient(CloudServiceExtended service, boolean serviceExist) {
        if (serviceExist) {
            when(client.getService(service.getName(), false)).thenReturn(service);
        }
    }

    protected void prepareContext(CloudServiceExtended service) {
        StepsUtil.setSpaceId(context, TEST_SPACE_ID);
        StepsUtil.setServiceToProcess(service, context);
    }

    private void prepareServiceInstanceGetter(CloudServiceExtended service, ServiceOperation serviceOperation) {
        if (serviceOperation != null) {
            when(serviceOperationGetter.getLastServiceOperation(any(), eq(service))).thenReturn(serviceOperation);
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
