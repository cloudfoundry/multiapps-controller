package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudMetadata;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableCloudService;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceInstance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceInstanceGetter;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceWithAlternativesCreator;
import com.sap.cloud.lm.sl.cf.core.exec.MethodExecution;
import com.sap.cloud.lm.sl.cf.core.exec.MethodExecution.ExecutionState;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

public class CreateServiceStepTest extends SyncFlowableStepTest<CreateServiceStep> {

    private static final String POLLING = "polling";
    private static final String STEP_EXECUTION = "stepExecution";
    private static final String DONE_EXECUTION_STATUS = "DONE";

    private StepInput stepInput;

    @Mock
    private ServiceInstanceGetter serviceInstanceGetter;
    @Mock
    private ServiceWithAlternativesCreator.Factory serviceCreatorFactory;

    // @formatter:off
    private static Stream<Arguments> testExecute() {
        return Stream.of(
                Arguments.of("create-service-step-input-1.json", null),
                Arguments.of("create-service-step-input-2-user-provided.json", null)
        );
    }
    // @formatter:on

    @ParameterizedTest
    @MethodSource
    public void testExecute(String stepInput, String expectedExceptionMessage) throws Exception {
        initializeInput(stepInput, expectedExceptionMessage);
        prepareResponses(STEP_EXECUTION);
        step.execute(context);
        assertStepPhase(STEP_EXECUTION);

        if (getExecutionStatus().equals(DONE_EXECUTION_STATUS)) {
            return;
        }
        prepareResponses(POLLING);
        step.execute(context);
        assertStepPhase(POLLING);
    }

    @Test
    public void testExceptionIsThrownOnManagedServiceCreationInternalServerError() {
        initializeInput("create-service-step-input-1.json", null);
        throwExceptionOnServiceCreation(HttpStatus.INTERNAL_SERVER_ERROR);
        Assertions.assertThrows(SLException.class, () -> step.execute(context));
    }

    @Test
    public void testExceptionIsThrownOnManagedServiceCreationBadGateway() {
        initializeInput("create-service-step-input-1.json", null);
        throwExceptionOnServiceCreation(HttpStatus.BAD_GATEWAY);
        Assertions.assertThrows(SLException.class, () -> step.execute(context));
    }

    @Test
    public void testWhenServiceAlreadyExists() {
        initializeInput("create-service-step-input-1.json", null);
        CloudService cloudService = Mockito.mock(CloudService.class);
        Mockito.when(client.getService(any(), eq(false)))
               .thenReturn(cloudService);
        step.execute(context);
        Assertions.assertEquals(DONE_EXECUTION_STATUS, getExecutionStatus());
    }

    private void throwExceptionOnServiceCreation(HttpStatus httpStatus) {
        ServiceWithAlternativesCreator serviceCreator = Mockito.mock(ServiceWithAlternativesCreator.class);
        Mockito.when(serviceCreator.createService(any(), any(), any()))
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
               .createUserProvidedService(any(CloudServiceExtended.class), any(Map.class));
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

        Mockito.when(serviceCreatorFactory.createInstance(any()))
               .thenReturn(serviceCreator);
        Mockito.when(serviceCreator.createService(any(), any(), any()))
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
                                            .service(ImmutableCloudService.builder()
                                                                          .name(service.name)
                                                                          .plan(service.plan)
                                                                          .label(service.label)
                                                                          .metadata(serviceMetadata)
                                                                          .build())
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
    }

}
