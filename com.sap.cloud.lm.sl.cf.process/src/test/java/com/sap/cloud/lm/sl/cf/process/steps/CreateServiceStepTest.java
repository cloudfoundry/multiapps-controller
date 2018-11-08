package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.lib.domain.CloudEntity.Meta;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceInstanceGetter;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceWithAlternativesCreator;
import com.sap.cloud.lm.sl.cf.core.exec.MethodExecution;
import com.sap.cloud.lm.sl.cf.core.exec.MethodExecution.ExecutionState;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class CreateServiceStepTest extends SyncFlowableStepTest<CreateServiceStep> {

    private static final String POLLING = "polling";
    private static final String STEP_EXECUTION = "stepExecution";

    private final StepInput stepInput;

    @Mock
    private ServiceInstanceGetter serviceInstanceGetter;
    @Mock
    private ServiceWithAlternativesCreator.Factory serviceCreatorFactory;

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
        // @formatter:off
            {
                "create-service-step-input-1.json", null,
            },
            {
                "create-service-step-input-2-user-provided.json", null,
            }
        // @formatter:on
        });
    }

    public CreateServiceStepTest(String stepInput, String expectedExceptionMessage) throws Exception {
        this.stepInput = JsonUtil.fromJson(TestUtil.getResourceAsString(stepInput, CreateServiceStepTest.class), StepInput.class);
    }

    @Before
    public void setUp() throws Exception {
        prepareContext();
        prepareClient();
    }

    @Test
    public void testExecute() throws Exception {
        prepareResponses(STEP_EXECUTION);
        step.execute(context);
        assertStepPhase(STEP_EXECUTION);

        if(getExecutionStatus().equals("DONE")) {
            return;
        }
        prepareResponses(POLLING);
        step.execute(context);
        assertStepPhase(POLLING);
    }

    @SuppressWarnings("unchecked")
    private void assertStepPhase(String stepPhase) {
        Map<String, Object> stepPhaseResults = (Map<String, Object>) stepInput.stepPhaseResults.get(stepPhase);
        String expectedStepPhase = (String) stepPhaseResults.get("expextedStepPhase");
        assertEquals(expectedStepPhase, getExecutionStatus());
    }

    private void prepareContext() {
        context.setVariable("serviceToProcess", JsonUtil.toJson(stepInput.service));
    }

    private void prepareClient() {
        SimpleService service = stepInput.service;
        Mockito.when(client.getServiceInstance(service.name))
            .thenReturn(createServiceInstance(service));
        Mockito.doNothing()
            .when(client)
            .createUserProvidedService(Matchers.any(CloudServiceExtended.class), Matchers.any(Map.class));
    }

    private void prepareFactory(String stepPhase) {
        Mockito.reset(serviceCreatorFactory);
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
        ServiceWithAlternativesCreator serviceCreator = Mockito.mock(ServiceWithAlternativesCreator.class);
        Mockito.when(serviceCreatorFactory.createInstance(Matchers.any()))
            .thenReturn(serviceCreator);
        Mockito.when(serviceCreator.createService(Matchers.any(), Matchers.any(), Matchers.any()))
            .thenReturn(methodExec);
    }

    private void prepareResponses(String stepPhase) {
        prepareFactory(stepPhase);
    }

    private CloudServiceInstance createServiceInstance(SimpleService service) {
        CloudServiceInstance instance = new CloudServiceInstance();
        CloudService cloudService = new CloudService();
        cloudService.setName(service.name);
        cloudService.setLabel(service.label);
        cloudService.setPlan(service.plan);
        UUID guid = UUID.fromString(service.guid);
        cloudService.setMeta(new Meta(guid, null, null));
        instance.setService(cloudService);
        return instance;
    }

    private static class StepInput {
        SimpleService service;
        Map<String, Object> stepPhaseResults;
    }

    private static class SimpleService {
        String name;
        String label;
        String plan;
        String guid;
    }

    @Override
    protected CreateServiceStep createStep() {
        return new CreateServiceStep();
    }

}
