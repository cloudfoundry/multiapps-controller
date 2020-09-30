package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import java.util.Map;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.core.cf.clients.ServiceInstanceGetter;
import org.cloudfoundry.multiapps.controller.core.cf.clients.ServiceUpdater;
import org.cloudfoundry.multiapps.controller.core.util.MethodExecution;
import org.cloudfoundry.multiapps.controller.core.util.MethodExecution.ExecutionState;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;

class UpdateServiceTagsStepTest extends SyncFlowableStepTest<UpdateServiceTagsStep> {

    private static final String POLLING = "polling";
    private static final String STEP_EXECUTION = "stepExecution";

    @Mock
    private ServiceInstanceGetter serviceInstanceGetter;
    @Mock
    protected ServiceUpdater serviceUpdater;

    public static Stream<Arguments> testExecute() {
        return Stream.of(
        // @formatter:off
            Arguments.of("update-service-tags-step-input-1.json")
        // @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExecute(String stepInputFilename) {
        StepInput input = JsonUtil.fromJson(TestUtil.getResourceAsString(stepInputFilename, UpdateServiceTagsStepTest.class),
                                            StepInput.class);
        prepareContext(input);

        prepareResponses(STEP_EXECUTION);
        step.execute(execution);
        assertStepPhase(input, STEP_EXECUTION);

        if (getExecutionStatus().equals("DONE")) {
            return;
        }
        prepareResponses(POLLING);
        step.execute(execution);
        assertStepPhase(input, POLLING);
        assertMethodCalls();
    }

    private void prepareServiceUpdater(String stepPhase) {
        MethodExecution<String> methodExec;
        switch (stepPhase) {
            case POLLING:
                methodExec = new MethodExecution<>(null, ExecutionState.FINISHED);
                break;
            case STEP_EXECUTION:
                methodExec = new MethodExecution<>(null, ExecutionState.EXECUTING);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported test phase");
        }
        Mockito.when(serviceUpdater.updateServiceTags(any(), any(), any()))
               .thenReturn(methodExec);
    }

    private void assertMethodCalls() {
        Mockito.verify(serviceUpdater, Mockito.times(1))
               .updateServiceTags(any(), any(), any());
    }

    @SuppressWarnings("unchecked")
    private void assertStepPhase(StepInput input, String stepPhase) {
        Map<String, Object> stepPhaseResults = (Map<String, Object>) input.stepPhaseResults.get(stepPhase);
        String expectedStepPhase = (String) stepPhaseResults.get("expectedStepPhase");
        assertEquals(expectedStepPhase, getExecutionStatus());
    }

    private void prepareContext(StepInput input) {
        execution.setVariable("serviceToProcess", JsonUtil.toJson(input.service));
    }

    private void prepareResponses(String stepPhase) {
        prepareServiceUpdater(stepPhase);
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
    protected UpdateServiceTagsStep createStep() {
        return new UpdateServiceTagsStep();
    }

}
