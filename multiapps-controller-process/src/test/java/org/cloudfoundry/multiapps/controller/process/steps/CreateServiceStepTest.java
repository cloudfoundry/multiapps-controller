package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudMetadata;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceInstance;
import org.cloudfoundry.client.lib.domain.ServiceInstanceType;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.cf.clients.ServiceWithAlternativesCreator;
import org.cloudfoundry.multiapps.controller.core.util.MethodExecution;
import org.cloudfoundry.multiapps.controller.core.util.MethodExecution.ExecutionState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

class CreateServiceStepTest extends SyncFlowableStepTest<CreateServiceStep> {

    private static final String POLLING = "polling";
    private static final String STEP_EXECUTION = "stepExecution";
    private static final String METADATA_UPDATE = "metadataUpdate";
    private static final String DONE_EXECUTION_STATUS = "DONE";

    private StepInput stepInput;

    @Mock
    private ServiceWithAlternativesCreator.Factory serviceCreatorFactory;

    static Stream<Arguments> testExecute() {
        return Stream.of(Arguments.of("create-service-step-input-1.json", null),
                         Arguments.of("create-service-step-input-2-user-provided.json", null));
    }

    @ParameterizedTest
    @MethodSource
    void testExecute(String stepInput, String expectedExceptionMessage) {
        initializeInput(stepInput, expectedExceptionMessage);
        prepareResponses(STEP_EXECUTION);
        step.execute(execution);
        assertStepPhase(STEP_EXECUTION);

        if (getExecutionStatus().equals(DONE_EXECUTION_STATUS)) {
            return;
        }
        prepareResponses(POLLING);
        step.execute(execution);
        assertStepPhase(POLLING);
        step.execute(execution);
        assertStepPhase(METADATA_UPDATE);
    }

    @Test
    void testExceptionIsThrownOnManagedServiceCreationInternalServerError() {
        initializeInput("create-service-step-input-1.json", null);
        throwExceptionOnServiceCreation(HttpStatus.INTERNAL_SERVER_ERROR);
        Assertions.assertThrows(SLException.class, () -> step.execute(execution));
    }

    @Test
    void testExceptionIsThrownOnManagedServiceCreationBadGateway() {
        initializeInput("create-service-step-input-1.json", null);
        throwExceptionOnServiceCreation(HttpStatus.BAD_GATEWAY);
        Assertions.assertThrows(SLException.class, () -> step.execute(execution));
    }

    @Test
    void testWhenServiceAlreadyExists() {
        initializeInput("create-service-step-input-1.json", null);
        CloudServiceInstance cloudService = Mockito.mock(CloudServiceInstance.class);
        Mockito.when(client.getServiceInstance(any(), eq(false)))
               .thenReturn(cloudService);
        step.execute(execution);
        assertEquals(DONE_EXECUTION_STATUS, getExecutionStatus());
    }

    private void throwExceptionOnServiceCreation(HttpStatus httpStatus) {
        ServiceWithAlternativesCreator serviceCreator = Mockito.mock(ServiceWithAlternativesCreator.class);
        Mockito.when(serviceCreator.createService(any(), any()))
               .thenThrow(new CloudOperationException(httpStatus, "Error occurred"));
        Mockito.when(serviceCreatorFactory.createInstance(any()))
               .thenReturn(serviceCreator);
    }

    private void initializeInput(String stepInput, String expectedExceptionMessage) {
        this.stepInput = JsonUtil.fromJson(TestUtil.getResourceAsString(stepInput, CreateServiceStepTest.class), StepInput.class);
        prepareContext();
        prepareClient();
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

    private void prepareClient() {
        SimpleService service = stepInput.service;
        CloudServiceInstance cloudService = createServiceInstance(service);
        Mockito.when(client.getServiceInstance(service.name))
               .thenReturn(cloudService);
        Mockito.doNothing()
               .when(client)
               .createUserProvidedServiceInstance(any(CloudServiceInstanceExtended.class), any(Map.class));

    }

    private void prepareFactory(String stepPhase) {
        Mockito.reset(serviceCreatorFactory);
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
        ServiceWithAlternativesCreator serviceCreator = Mockito.mock(ServiceWithAlternativesCreator.class);

        Mockito.when(serviceCreatorFactory.createInstance(any()))
               .thenReturn(serviceCreator);
        Mockito.when(serviceCreator.createService(any(), any()))
               .thenReturn(methodExec);
    }

    private void prepareResponses(String stepPhase) {
        prepareFactory(stepPhase);
    }

    private CloudServiceInstance createServiceInstance(SimpleService service) {
        CloudMetadata serviceMetadata = ImmutableCloudMetadata.builder()
                                                              .guid(UUID.fromString(service.guid))
                                                              .build();
        return ImmutableCloudServiceInstance.builder()
                                            .name(service.name)
                                            .plan(service.plan)
                                            .label(service.label)
                                            .metadata(serviceMetadata)
                                            .type(ServiceInstanceType.valueOfWithDefault(service.type))
                                            .build();
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
