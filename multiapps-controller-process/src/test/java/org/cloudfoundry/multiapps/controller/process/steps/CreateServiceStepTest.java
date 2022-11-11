package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.cloudfoundry.client.v3.serviceinstances.ServiceInstanceType;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
    private static final String METADATA_UPDATE = "metadataUpdate";
    private static final String SERVICE_NAME = "service-1";
    private static final String SERVICE_LOG_DRAIN = "syslogDrain";

    private static final Map<String, Object> CREDENTIALS = Map.of("testCredentialsKey", "testCredentialsValue");
    private static final List<String> SERVICE_TAGS = List.of("custom-tag-A", "custom-tag-B");

    private static final Map<String, StepPhase> MANAGED_SERVICE_STEPS = Map.of(STEP_EXECUTION, StepPhase.POLL, POLLING, StepPhase.POLL,
                                                                               METADATA_UPDATE, StepPhase.DONE);

    private StepInput stepInput;

    static Stream<Arguments> testExecute() {
        return Stream.of(Arguments.of(createCloudService(), MANAGED_SERVICE_STEPS, false),
                         Arguments.of(createUserProvidedCloudService(), Map.of(STEP_EXECUTION, StepPhase.DONE), false));
    }

    @ParameterizedTest
    @MethodSource
    void testExecute(CloudServiceInstance service, Map<String, StepPhase> stepPhaseResults, boolean serviceExists) {
        initializeInput(service, stepPhaseResults, serviceExists);
        step.execute(execution);
        assertStepPhase(STEP_EXECUTION);

        if (getExecutionStatus().equals(StepPhase.DONE.toString())) {
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
        initializeInput(createCloudService(), MANAGED_SERVICE_STEPS, false);
        throwExceptionOnServiceCreation(HttpStatus.INTERNAL_SERVER_ERROR);
        Assertions.assertThrows(SLException.class, () -> step.execute(execution));
    }

    @Test
    void testExceptionIsThrownOnManagedServiceCreationBadGateway() {
        initializeInput(createCloudService(), MANAGED_SERVICE_STEPS, false);
        throwExceptionOnServiceCreation(HttpStatus.BAD_GATEWAY);
        Assertions.assertThrows(SLException.class, () -> step.execute(execution));
    }

    private static Stream<Arguments> testCreateServiceInstanceWhenAlreadyExists() {
        return Stream.of(Arguments.of(ServiceOperation.State.SUCCEEDED, StepPhase.DONE.toString()),
                         Arguments.of(ServiceOperation.State.INITIAL, StepPhase.POLL.toString()),
                         Arguments.of(ServiceOperation.State.IN_PROGRESS, StepPhase.POLL.toString()));
    }

    @MethodSource
    @ParameterizedTest
    void testCreateServiceInstanceWhenAlreadyExists(ServiceOperation.State serviceInstanceState, String executionStatus) {
        initializeInput(createCloudService(), MANAGED_SERVICE_STEPS, true);
        ImmutableCloudServiceInstance existingCloudServiceInstance = ImmutableCloudServiceInstance.copyOf(createCloudService())
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

    private void throwExceptionOnServiceCreation(HttpStatus httpStatus) {
        Mockito.doThrow(new CloudOperationException(httpStatus, "Error occurred"))
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

    private static CloudServiceInstance createCloudService() {
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .metadata(ImmutableCloudMetadata.builder()
                                                                                    .guid(UUID.randomUUID())
                                                                                    .build())
                                                    .name(SERVICE_NAME)
                                                    .label("label-1")
                                                    .plan("plan-1")
                                                    .build();
    }

    private static CloudServiceInstance createUserProvidedCloudService() {
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .name(SERVICE_NAME)
                                                    .type(ServiceInstanceType.USER_PROVIDED)
                                                    .syslogDrainUrl(SERVICE_LOG_DRAIN)
                                                    .credentials(CREDENTIALS)
                                                    .tags(SERVICE_TAGS)
                                                    .build();
    }

}
