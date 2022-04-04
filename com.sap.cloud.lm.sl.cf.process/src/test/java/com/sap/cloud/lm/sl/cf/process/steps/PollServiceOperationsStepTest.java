package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceInstanceGetter;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class PollServiceOperationsStepTest extends AsyncStepOperationTest<CreateOrUpdateServicesStep> {

    private static final String TEST_SPACE_ID = "test";
    @Rule
    public ExpectedException exception = ExpectedException.none();
    @Mock
    protected CloudControllerClient client;
    @Mock
    private ServiceInstanceGetter serviceInstanceGetter;
    private StepInput input;
    private String expectedExceptionMessage;
    public PollServiceOperationsStepTest(String inputLocation, String expectedExceptionMessage) throws ParsingException, IOException {
        this.input = JsonUtil.fromJson(TestUtil.getResourceAsString(inputLocation, PollServiceOperationsStepTest.class), StepInput.class);
        this.expectedExceptionMessage = expectedExceptionMessage;
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) With no async services:
            {
                "poll-create-services-step-input-00.json", null
            },
            // (1) With one async service:
            {
                "poll-create-services-step-input-01.json", null
            },
            // (2) With non-existing service:
            {
                "poll-create-services-step-input-02.json", "Cannot retrieve instance of service test-service-2"
            },
            // (3) With non-existing optional service:
            {
                "poll-create-services-step-input-03.json", null
            },
            // (4) With failure and optional service:
            {
                "poll-create-services-step-input-04.json", null
            },
            // (5) With failure and optional service:
            {
                "poll-create-services-step-input-05.json", "Error creating service \"test-service-2\" from offering \"test\" and plan \"test\": Something happened!"
            },
            // (6) With user provided service:
            {
                "poll-create-services-step-input-06.json", null
            },
            // (7) With failure on update of service:
            {
                "poll-create-services-step-input-07.json", null
            },
            // (8) With failure on creation of service and update of service:
            {
                "poll-create-services-step-input-08.json", "Error creating service \"test-service-2\" from offering \"test\" and plan \"test\": Something happened!"
            },
            // (8) With failure on creation of service and no error description:
            {
                "poll-create-services-step-input-09.json", "Error creating service \"test-service\" from offering \"test\" and plan \"test\": " + Messages.DEFAULT_FAILED_OPERATION_DESCRIPTION
            },
// @formatter:on
        });
    }

    @Before
    public void setUp() {
        context.setVariable(com.sap.cloud.lm.sl.cf.persistence.message.Constants.VARIABLE_NAME_SPACE_ID, TEST_SPACE_ID);
        prepareServiceInstanceGetter();
        StepsUtil.setServicesToCreate(context, input.services);
        StepsUtil.setServicesToDelete(context, Collections.emptyList());
        StepsUtil.setServicesData(context, Collections.emptyMap());
        StepsUtil.setTriggeredServiceOperations(context, input.triggeredServiceOperations);
        if (expectedExceptionMessage != null) {
            exception.expectMessage(expectedExceptionMessage);
        }
        context.setVariable(Constants.VAR_SERVICES_TO_CREATE_COUNT, 0);
        when(clientProvider.getControllerClient(anyString(), anyString())).thenReturn(client);
    }

    @SuppressWarnings("unchecked")
    private void prepareServiceInstanceGetter() {
        for (Entry<String, Object> response : input.serviceInstanceResponse.entrySet()) {
            Mockito.when(serviceInstanceGetter.getServiceInstanceEntity(client, response.getKey(), TEST_SPACE_ID))
                   .thenReturn((Map<String, Object>) response.getValue());
        }
    }

    @Override
    protected void validateOperationExecutionResult(AsyncExecutionState result) {
        assertEquals(input.expectedStatus, result.toString());
    }

    @Override
    protected CreateOrUpdateServicesStep createStep() {
        return new CreateOrUpdateServicesStep();
    }

    @Override
    protected List<AsyncExecution> getAsyncOperations(ExecutionWrapper wrapper) {
        return step.getAsyncStepExecutions(wrapper);
    }

    private static class StepInput {
        List<CloudServiceExtended> services;
        Map<String, ServiceOperationType> triggeredServiceOperations;
        Map<String, Object> serviceInstanceResponse;
        String expectedStatus;
    }

}
