package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ServiceOperationGetter;
import org.cloudfoundry.multiapps.controller.process.util.ServiceProgressReporter;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.ServiceOperation;

class PollServiceOperationsStepTest extends AsyncStepOperationTest<CreateServiceStep> {

    private static final String TEST_SPACE_ID = "test";

    @Mock
    private ServiceOperationGetter serviceOperationGetter;
    @Mock
    private ServiceProgressReporter serviceProgressReporter;
    @Mock
    protected CloudControllerClient client;

    private StepInput input;

    public static Stream<Arguments> testPollStateExecution() {
        return Stream.of(
// @formatter:off
            // (0) With no async services:
            Arguments.of("poll-create-services-step-input-00.json", null),
            // (1) With one async service:
            Arguments.of("poll-create-services-step-input-01.json", null),
            // (2) With non-existing service:
            Arguments.of("poll-create-services-step-input-02.json", "Cannot retrieve service instance of service \"test-service-2\""),
            // (3) With non-existing optional service:
            Arguments.of("poll-create-services-step-input-03.json", null),
            // (4) With failure and optional service:
            Arguments.of("poll-create-services-step-input-04.json", null),
            // (5) With failure and optional service:
            Arguments.of("poll-create-services-step-input-05.json", "Error creating service \"test-service-2\" from offering \"test\" and plan \"test\": Something happened!"),
            // (6) With user provided service:
            Arguments.of("poll-create-services-step-input-06.json", null),
            // (7) With failure on update of service:
            Arguments.of("poll-create-services-step-input-07.json", null),
            // (8) With failure on creation of service and update of service:
            Arguments.of("poll-create-services-step-input-08.json", "Error creating service \"test-service-2\" from offering \"test\" and plan \"test\": Something happened!"),
            // (8) With failure on creation of service and no error description:
            Arguments.of("poll-create-services-step-input-09.json", "Error creating service \"test-service\" from offering \"test\" and plan \"test\": " + Messages.DEFAULT_FAILED_OPERATION_DESCRIPTION)
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testPollStateExecution(String inputLocation, String expectedExceptionMessage) {
        this.input = JsonUtil.fromJson(TestUtil.getResourceAsString(inputLocation, PollServiceOperationsStepTest.class), StepInput.class);
        initializeParameters();
        if (expectedExceptionMessage != null) {
            Exception exception = assertThrows(Exception.class, this::testExecuteOperations);
            assertTrue(exception.getMessage()
                                .contains(expectedExceptionMessage));
            return;
        }
        testExecuteOperations();
    }

    private void initializeParameters() {
        context.setVariable(Variables.SPACE_GUID, TEST_SPACE_ID);
        prepareServiceOperationGetter();
        context.setVariable(Variables.SERVICES_TO_CREATE, input.services);
        context.setVariable(Variables.SERVICES_TO_DELETE, Collections.emptyList());
        context.setVariable(Variables.SERVICES_DATA, Collections.emptyList());
        context.setVariable(Variables.TRIGGERED_SERVICE_OPERATIONS, input.triggeredServiceOperations);

        context.setVariable(Variables.SERVICES_TO_CREATE_COUNT, 0);
        when(clientProvider.getControllerClient(anyString(), anyString(), anyString())).thenReturn(client);
    }

    @SuppressWarnings("unchecked")
    private void prepareServiceOperationGetter() {
        for (Entry<String, Object> response : input.serviceInstanceResponse.entrySet()) {
            Map<String, Object> serviceInstanceResponse = (Map<String, Object>) response.getValue();
            if (serviceInstanceResponse == null) {
                continue;
            }
            Map<String, Object> serviceOperationAsMap = (Map<String, Object>) serviceInstanceResponse.get("last_operation");
            CloudServiceInstanceExtended service = getCloudServiceExtended(response);
            ServiceOperation lastOp = new ServiceOperation(ServiceOperation.Type.fromString((String) serviceOperationAsMap.get("type")),
                                                           (String) serviceOperationAsMap.get("description"),
                                                           ServiceOperation.State.fromString((String) serviceOperationAsMap.get("state")));

            when(serviceOperationGetter.getLastServiceOperation(any(), eq(service))).thenReturn(lastOp);

        }
    }

    private CloudServiceInstanceExtended getCloudServiceExtended(Entry<String, Object> response) {
        return input.services.stream()
                             .filter(serviceToFind -> serviceToFind.getName()
                                                                   .equals(response.getKey()))
                             .findFirst()
                             .get();
    }

    @Override
    protected void validateOperationExecutionResult(AsyncExecutionState result) {
        assertEquals(input.expectedStatus, result.toString());
    }

    private static class StepInput {
        List<CloudServiceInstanceExtended> services;
        Map<String, ServiceOperation.Type> triggeredServiceOperations;
        Map<String, Object> serviceInstanceResponse;
        String expectedStatus;
    }

    @Override
    protected CreateServiceStep createStep() {
        return new CreateServiceStep();
    }

    @Override
    protected List<AsyncExecution> getAsyncOperations(ProcessContext wrapper) {
        return List.of(new PollServiceCreateOrUpdateOperationsExecution(serviceOperationGetter, serviceProgressReporter));
    }

}
