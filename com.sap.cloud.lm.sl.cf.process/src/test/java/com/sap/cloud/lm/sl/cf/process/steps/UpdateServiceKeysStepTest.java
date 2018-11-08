package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.ServiceKey;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceInstanceGetter;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceUpdater;
import com.sap.cloud.lm.sl.cf.core.exec.MethodExecution;
import com.sap.cloud.lm.sl.cf.core.exec.MethodExecution.ExecutionState;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class UpdateServiceKeysStepTest extends SyncFlowableStepTest<UpdateServiceKeysStep> {

    private static final String POLLING = "polling";
    private static final String STEP_EXECUTION = "stepExecution";

    private final StepInput stepInput;

    @Mock
    private ServiceInstanceGetter serviceInstanceGetter;
    @Mock
    protected ServiceUpdater serviceUpdater;

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
        // @formatter:off
            {
                "update-service-keys-step-input-1.json", null,
            },
        // @formatter:on
        });
    }

    public UpdateServiceKeysStepTest(String stepInput, String expectedExceptionMessage) throws Exception {
        this.stepInput = JsonUtil.fromJson(TestUtil.getResourceAsString(stepInput, UpdateServiceKeysStepTest.class), StepInput.class);
    }

    @Before
    public void setUp() throws Exception {
        prepareContext();
        prepareClient();
    }

    private void prepareClient() {
        Mockito.when(client.getServiceKeys(stepInput.service.name)).thenReturn(stepInput.existingServiceKeys);
    }

    private void prepareServiceUpdater(String stepPhase) {
        MethodExecution<String> methodExec;
        switch (stepPhase) {
            case POLLING:
                methodExec = new MethodExecution<String>(null, ExecutionState.FINISHED);
                break;
            case STEP_EXECUTION:
                methodExec = new MethodExecution<String>(null, ExecutionState.EXECUTING);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported test phase");
        }
        Mockito.when(serviceUpdater.updateServiceTags(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(methodExec);
    }

    @Test
    public void testExecute() throws Exception {
        prepareResponses(STEP_EXECUTION);
        step.execute(context);
        assertStepPhase(STEP_EXECUTION);

        if (getExecutionStatus().equals("DONE")) {
            return;
        }
        assertMethodCalls();
    }
    
    private void assertMethodCalls() {
        Mockito.verify(serviceUpdater, Mockito.times(1)).updateServiceTags(Matchers.any(), Matchers.any(), Matchers.any());
    }

    @SuppressWarnings("unchecked")
    private void assertStepPhase(String stepPhase) {
        Map<String, Object> stepPhaseResults = (Map<String, Object>) stepInput.stepPhaseResults.get(stepPhase);
        String expectedStepPhase = (String) stepPhaseResults.get("expextedStepPhase");
        assertEquals(expectedStepPhase, getExecutionStatus());
    }

    private void prepareContext() {
        Map<String, List<ServiceKey>> keysToCreate = new HashMap<>();
        keysToCreate.put(stepInput.service.name, stepInput.serviceKeysToCreate);
        StepsUtil.setServiceKeysToCreate(context, keysToCreate);
        context.setVariable("serviceToProcess", JsonUtil.toJson(stepInput.service));
        context.setVariable("deleteServiceKeys", true);
    }

    private void prepareResponses(String stepPhase) {
        prepareServiceUpdater(stepPhase);
    }


    private static class StepInput {
        SimpleService service;
        List<ServiceKey> serviceKeysToCreate = Collections.emptyList();
        List<ServiceKey> existingServiceKeys = Collections.emptyList();
        Map<String, Object> stepPhaseResults;
    }

    private static class SimpleService {
        String name;
        String label;
        String plan;
        String guid;
    }

    @Override
    protected UpdateServiceKeysStep createStep() {
        return new UpdateServiceKeysStep();
    }

}
