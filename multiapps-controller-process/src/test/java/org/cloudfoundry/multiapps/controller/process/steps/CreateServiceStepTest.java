package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudOperationException;

class CreateServiceStepTest extends SyncFlowableStepTest<CreateServiceStep> {

    private static final String POLLING = "polling";
    private static final String STEP_EXECUTION = "stepExecution";
    private static final String METADATA_UPDATE = "metadataUpdate";
    private static final String DONE_EXECUTION_STATUS = "DONE";

    private StepInput stepInput;

    static Stream<Arguments> testExecute() {
        return Stream.of(Arguments.of("create-service-step-input-1.json", false),
                         Arguments.of("create-service-step-input-2-user-provided.json", false));
    }

    @ParameterizedTest
    @MethodSource
    void testExecute(String stepInput, boolean serviceExists) {
        initializeInput(stepInput, serviceExists);
        step.execute(execution);
        assertStepPhase(STEP_EXECUTION);

        if (getExecutionStatus().equals(DONE_EXECUTION_STATUS)) {
            return;
        }
        prepareClient(true);
        step.execute(execution);
        assertStepPhase(POLLING);
        step.execute(execution);
        assertStepPhase(METADATA_UPDATE);
    }

    @Test
    void testExceptionIsThrownOnManagedServiceCreationInternalServerError() {
        initializeInput("create-service-step-input-1.json", false);
        throwExceptionOnServiceCreation(HttpStatus.INTERNAL_SERVER_ERROR);
        Assertions.assertThrows(SLException.class, () -> step.execute(execution));
    }

    @Test
    void testExceptionIsThrownOnManagedServiceCreationBadGateway() {
        initializeInput("create-service-step-input-1.json", false);
        throwExceptionOnServiceCreation(HttpStatus.BAD_GATEWAY);
        Assertions.assertThrows(SLException.class, () -> step.execute(execution));
    }

    @Test
    void testWhenServiceAlreadyExists() {
        initializeInput("create-service-step-input-1.json", true);
        Mockito.when(client.getRequiredServiceInstanceGuid(anyString()))
               .thenReturn(UUID.randomUUID());
        step.execute(execution);
        assertEquals(DONE_EXECUTION_STATUS, getExecutionStatus());
    }

    private void throwExceptionOnServiceCreation(HttpStatus httpStatus) {
        Mockito.doThrow(new CloudOperationException(httpStatus, "Error occurred"))
               .when(client)
               .createServiceInstance(any());
    }

    private void initializeInput(String stepInput, boolean serviceExists) {
        this.stepInput = JsonUtil.fromJson(TestUtil.getResourceAsString(stepInput, CreateServiceStepTest.class), StepInput.class);
        prepareContext();
        prepareClient(serviceExists);
    }

    @SuppressWarnings("unchecked")
    private void assertStepPhase(String stepPhase) {
        Map<String, Object> stepPhaseResults = (Map<String, Object>) stepInput.stepPhaseResults.get(stepPhase);
        String expectedStepPhase = (String) stepPhaseResults.get("expectedStepPhase");
        assertEquals(expectedStepPhase, getExecutionStatus());
    }

    private void prepareContext() {
        execution.setVariable("serviceToProcess", JsonUtil.toJson(stepInput.service));
    }

    private void prepareClient(boolean serviceExists) {
        Mockito.reset(client);
        if (serviceExists) {
            SimpleService service = stepInput.service;
            Mockito.when(client.getRequiredServiceInstanceGuid(service.name))
                   .thenReturn(UUID.fromString(service.guid));
        } else {
            Mockito.when(client.getRequiredServiceInstanceGuid(anyString()))
                   .thenThrow(new CloudOperationException(HttpStatus.NOT_FOUND));
        }
    }

    @Override
    protected CreateServiceStep createStep() {
        return new CreateServiceStep();
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
        String type;
    }

}
