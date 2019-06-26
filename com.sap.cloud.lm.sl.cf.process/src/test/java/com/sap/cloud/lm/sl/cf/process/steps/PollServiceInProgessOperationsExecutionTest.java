package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Arrays;
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
import com.sap.cloud.lm.sl.cf.core.cf.clients.EventsGetter;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceGetter;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.persistence.message.Constants;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class PollServiceInProgessOperationsExecutionTest extends AsyncStepOperationTest<CheckForOperationsInProgressStep> {

    private static final String TEST_SPACE_ID = "test";

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) With 2 services in progress:
            {
                "poll-check-in-progress-services-step-input-0.json", false, null
            },
            // (1) With 1 service in progress state and 1 successfully deleted
            {
                "poll-check-in-progress-services-step-input-1.json", true, null
            },
            // (2) With 2 services created successfully and 1 user-provided
            {
                "poll-check-in-progress-services-step-input-2.json", false, null
            },
            // (3) Handle missing response for last service operation
            {
                "poll-check-in-progress-services-step-input-3.json", false, "Cannot retrieve service instance of service \"test-service-2\""
            },
// @formatter:on
        });
    }

    public PollServiceInProgessOperationsExecutionTest(String inputLocation, boolean wasServiceDeleted, String expectedExceptionMessage) {
        this.input = JsonUtil.fromJson(TestUtil.getResourceAsString(inputLocation, PollServiceOperationsStepTest.class), StepInput.class);
        this.wasServiceDeleted = wasServiceDeleted;
        this.expectedExceptionMessage = expectedExceptionMessage;
    }

    @Mock
    private ServiceGetter serviceInstanceGetter;
    @Mock
    private EventsGetter eventsGetter;
    @Mock
    private CloudControllerClient client;
    @Rule
    public ExpectedException exception = ExpectedException.none();
    private StepInput input;
    private boolean wasServiceDeleted;
    private String expectedExceptionMessage;

    @Before
    public void setUp() {
        context.setVariable(Constants.VARIABLE_NAME_SPACE_ID, TEST_SPACE_ID);
        prepareServiceInstanceGetter();
        prepareServicesData();
        prepareEventsGetter();
        if (expectedExceptionMessage != null) {
            exception.expectMessage(expectedExceptionMessage);
        }
        StepsUtil.setTriggeredServiceOperations(context, input.triggeredServiceOperations);
        when(clientProvider.getControllerClient(anyString(), anyString())).thenReturn(client);
    }

    @SuppressWarnings("unchecked")
    private void prepareServiceInstanceGetter() {
        for (Entry<String, Object> response : input.serviceInstanceResponse.entrySet()) {
            Mockito.when(serviceInstanceGetter.getServiceInstanceEntity(client, response.getKey(), TEST_SPACE_ID))
                .thenReturn((Map<String, Object>) response.getValue());
        }
    }

    private void prepareServicesData() {
        StepsUtil.setServicesData(context, input.services);
    }

    private void prepareEventsGetter() {
        input.services.forEach(service -> Mockito.when(eventsGetter.isDeleteEvent(anyString()))
            .thenReturn(wasServiceDeleted));
    }

    @Override
    protected CheckForOperationsInProgressStep createStep() {
        return new CheckForOperationsInProgressStep();
    }

    @Override
    protected void validateOperationExecutionResult(AsyncExecutionState result) {
        assertEquals(input.expectedStatus, result.toString());
    }

    private static class StepInput {
        List<CloudServiceExtended> services;
        Map<String, ServiceOperationType> triggeredServiceOperations;
        Map<String, Object> serviceInstanceResponse;
        String expectedStatus;
    }

    @Override
    protected List<AsyncExecution> getAsyncOperations(ExecutionWrapper wrapper) {
        return step.getAsyncStepExecutions(wrapper);
    }
}
