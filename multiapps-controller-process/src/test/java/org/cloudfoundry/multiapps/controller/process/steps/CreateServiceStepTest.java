package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.cloudfoundry.client.v3.serviceinstances.ServiceInstanceType;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.model.DynamicResolvableParameter;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDynamicResolvableParameter;
import org.cloudfoundry.multiapps.controller.process.util.ServiceOperationGetter;
import org.cloudfoundry.multiapps.controller.process.util.ServiceProgressReporter;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceInstance;
import com.sap.cloudfoundry.client.facade.domain.ServiceOperation;

class CreateServiceStepTest extends SyncFlowableStepTest<CreateServiceStep> {

    private static final String POLLING = "polling";
    private static final String STEP_EXECUTION = "stepExecution";
    private static final String SERVICE_NAME = "service-1";
    private static final String SERVICE_LOG_DRAIN = "syslogDrain";

    private static final Map<String, Object> CREDENTIALS = Map.of("testCredentialsKey", "testCredentialsValue");
    private static final List<String> SERVICE_TAGS = List.of("custom-tag-A", "custom-tag-B");

    private static final Map<String, StepPhase> MANAGED_SERVICE_STEPS = Map.of(STEP_EXECUTION, StepPhase.POLL, POLLING, StepPhase.POLL);

    @Mock
    private ServiceOperationGetter serviceOperationGetter;
    @Mock
    private ServiceProgressReporter serviceProgressReporter;

    private StepInput stepInput;

    static Stream<Arguments> testExecute() {
        return Stream.of(Arguments.of(createCloudService(UUID.randomUUID()), MANAGED_SERVICE_STEPS, false),
                         Arguments.of(createUserProvidedCloudService(), Map.of(STEP_EXECUTION, StepPhase.DONE), false));
    }

    private static Stream<Arguments> testCreateServiceInstanceWhenAlreadyExists() {
        return Stream.of(Arguments.of(ServiceOperation.State.SUCCEEDED, StepPhase.DONE.toString()),
                         Arguments.of(ServiceOperation.State.INITIAL, StepPhase.POLL.toString()),
                         Arguments.of(ServiceOperation.State.IN_PROGRESS, StepPhase.POLL.toString()));
    }

    static Stream<Arguments> testSetServiceGuidIfPresent() {
        return Stream.of(Arguments.of(
                                      // (1) Test resolve service guid
                                      Set.of(ImmutableDynamicResolvableParameter.builder()
                                                                                .parameterName("service-guid")
                                                                                .relationshipEntityName("service-1")
                                                                                .build(),
                                             ImmutableDynamicResolvableParameter.builder()
                                                                                .parameterName("service-guid")
                                                                                .relationshipEntityName("service-2")
                                                                                .build()),
                                      ImmutableDynamicResolvableParameter.builder()
                                                                         .parameterName("service-guid")
                                                                         .relationshipEntityName("service-1")
                                                                         .value("beeb5e8d-4ab9-46ee-9205-455a278743f0")
                                                                         .build()),
                         // (2) Test skip resolve of unrelated parameter
                         Arguments.of(Set.of(ImmutableDynamicResolvableParameter.builder()
                                                                                .parameterName("service-guid")
                                                                                .relationshipEntityName("service-2")
                                                                                .build()),
                                      null),
                         // (3) Test skip resolve of unrelated parameter due to different parameter type
                         Arguments.of(Set.of(ImmutableDynamicResolvableParameter.builder()
                                                                                .parameterName("metadata-key")
                                                                                .relationshipEntityName("service-1")
                                                                                .build()),
                                      null));
    }

    private static CloudServiceInstanceExtended createCloudService(UUID serviceGuid) {
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .metadata(ImmutableCloudMetadata.builder()
                                                                                    .guid(serviceGuid)
                                                                                    .build())
                                                    .name(SERVICE_NAME)
                                                    .resourceName(SERVICE_NAME)
                                                    .label("label-1")
                                                    .plan("plan-1")
                                                    .build();
    }

    private static CloudServiceInstance createUserProvidedCloudService() {
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .name(SERVICE_NAME)
                                                    .resourceName(SERVICE_NAME)
                                                    .type(ServiceInstanceType.USER_PROVIDED)
                                                    .syslogDrainUrl(SERVICE_LOG_DRAIN)
                                                    .credentials(CREDENTIALS)
                                                    .tags(SERVICE_TAGS)
                                                    .build();
    }

    @ParameterizedTest
    @MethodSource
    void testExecute(CloudServiceInstanceExtended service, Map<String, StepPhase> stepPhaseResults, boolean serviceExists) {
        initializeInput(service, stepPhaseResults, serviceExists);
        step.execute(execution);
        assertStepPhase(STEP_EXECUTION);

        if (getExecutionStatus().equals(StepPhase.DONE.toString())) {
            return;
        }
        prepareClient(true);
        prepareServiceOperationsGetter(service);
        step.execute(execution);
        assertStepPhase(POLLING);
    }

    private void prepareServiceOperationsGetter(CloudServiceInstanceExtended service) {
        context.setVariable(Variables.SERVICES_TO_CREATE, List.of(service));
        when(serviceOperationGetter.getLastServiceOperation(any(),
                                                            any())).thenReturn(new ServiceOperation(ServiceOperation.Type.CREATE,
                                                                                                    "create done",
                                                                                                    ServiceOperation.State.IN_PROGRESS));
    }

    @Test
    void testExceptionIsThrownOnManagedServiceCreationInternalServerError() {
        initializeInput(createCloudService(UUID.randomUUID()), MANAGED_SERVICE_STEPS, false);
        throwExceptionOnServiceCreation(HttpStatus.INTERNAL_SERVER_ERROR);
        Assertions.assertThrows(SLException.class, () -> step.execute(execution));
    }

    @Test
    void testExceptionIsThrownOnManagedServiceCreationUnprocessableEntity() {
        initializeInput(createCloudService(UUID.randomUUID()), MANAGED_SERVICE_STEPS, false);
        throwExceptionOnServiceCreation(HttpStatus.UNPROCESSABLE_ENTITY);
        Exception exception = assertThrows(SLException.class, () -> step.execute(execution));
        assertEquals("Service operation failed: Controller operation failed: 422 Updating service \"service-1\" failed: Error occurred: Error creating or updating service instance: Could not create service \"service-1\" : Expected Exception message ",
                     exception.getMessage());
    }

    @Test
    void testExceptionIsThrownOnManagedServiceCreationBadGateway() {
        initializeInput(createCloudService(UUID.randomUUID()), MANAGED_SERVICE_STEPS, false);
        throwExceptionOnServiceCreation(HttpStatus.BAD_GATEWAY);
        Assertions.assertThrows(SLException.class, () -> step.execute(execution));
    }

    @MethodSource
    @ParameterizedTest
    void testCreateServiceInstanceWhenAlreadyExists(ServiceOperation.State serviceInstanceState, String executionStatus) {
        initializeInput(createCloudService(UUID.randomUUID()), MANAGED_SERVICE_STEPS, true);
        ImmutableCloudServiceInstance existingCloudServiceInstance = ImmutableCloudServiceInstance.copyOf(createCloudService(UUID.randomUUID()))
                                                                                                  .withLastOperation(createLastOperation(serviceInstanceState));
        Mockito.when(client.getServiceInstanceWithoutAuxiliaryContent(anyString(), anyBoolean()))
               .thenReturn(existingCloudServiceInstance);
        Mockito.doThrow(new CloudOperationException(HttpStatus.UNPROCESSABLE_ENTITY))
               .when(client)
               .createServiceInstance(any());
        step.execute(execution);
        assertEquals(executionStatus, getExecutionStatus());
    }

    private ServiceOperation createLastOperation(ServiceOperation.State state) {
        return new ServiceOperation(ServiceOperation.Type.CREATE, null, state);
    }

    @Test
    void testUserProvidedParametersParsing() {
        CloudServiceInstance userProvidedService = createUserProvidedCloudService();
        initializeInput(userProvidedService, Map.of(STEP_EXECUTION, StepPhase.DONE), false);
        step.execute(execution);
        Mockito.verify(client, times(1))
               .createUserProvidedServiceInstance(userProvidedService);
    }

    @ParameterizedTest
    @MethodSource
    void testSetServiceGuidIfPresent(Set<DynamicResolvableParameter> dynamicResolvableParameters,
                                     DynamicResolvableParameter expectedResolvedParameter) {
        initializeInput(createCloudService(UUID.fromString("beeb5e8d-4ab9-46ee-9205-455a278743f0")), null, true);
        context.setVariable(Variables.DYNAMIC_RESOLVABLE_PARAMETERS, dynamicResolvableParameters);

        step.execute(execution);

        assertEquals(expectedResolvedParameter, context.getVariable(Variables.DYNAMIC_RESOLVABLE_PARAMETER));
    }

    private void throwExceptionOnServiceCreation(HttpStatus httpStatus) {
        Mockito.doThrow(new CloudOperationException(httpStatus, "Error occurred", "Expected Exception message"))
               .when(client)
               .createServiceInstance(any());
    }

    private void initializeInput(CloudServiceInstance service, Map<String, StepPhase> stepPhaseResults, boolean serviceExists) {
        this.stepInput = new StepInput(service, stepPhaseResults);
        prepareContext();
        prepareClient(serviceExists);
    }

    private void assertStepPhase(String stepPhase) {
        String expectedStepPhase = stepInput.stepPhaseResults.get(stepPhase)
                                                             .toString();
        assertEquals(expectedStepPhase, getExecutionStatus());
    }

    private void prepareContext() {
        execution.setVariable("serviceToProcess", JsonUtil.toJson(stepInput.service));
    }

    private void prepareClient(boolean serviceExists) {
        Mockito.reset(client);
        if (serviceExists) {
            CloudServiceInstance service = stepInput.service;
            Mockito.when(client.getRequiredServiceInstanceGuid(service.getName()))
                   .thenReturn((service.getGuid()));
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
        CloudServiceInstance service;
        Map<String, StepPhase> stepPhaseResults;

        StepInput(CloudServiceInstance service, Map<String, StepPhase> stepPhaseResults) {
            this.service = service;
            this.stepPhaseResults = stepPhaseResults;
        }

    }

}
