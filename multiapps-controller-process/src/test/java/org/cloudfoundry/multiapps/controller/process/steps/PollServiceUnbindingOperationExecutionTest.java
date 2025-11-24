package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;
import java.util.UUID;
import org.cloudfoundry.client.v3.jobs.JobState;
import org.cloudfoundry.client.v3.serviceinstances.ServiceInstanceType;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudAsyncJob;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudAsyncJob;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudMetadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PollServiceUnbindingOperationExecutionTest extends AsyncStepOperationTest<UnbindServiceFromApplicationStep> {

    private static final UUID JOB_GUID = UUID.fromString("00000000-0000-0000-0000-000000000123");
    private static final String JOB_ID = JOB_GUID.toString();
    private static final String APP_NAME = "test-app";
    private static final String SERVICE_NAME = "test-service";
    private static final String SERVICE_OFFERING = "test-offering";
    private static final String SERVICE_PLAN = "test-plan";
    private static final String ERROR_MESSAGE = "Test error";

    private AsyncExecutionState expectedAsyncExecutionState;

    @Test
    void testGetInProgressHandler() {
        prepareContext();
        CloudAsyncJob asyncJob = buildAsyncJobWithState(JobState.PROCESSING);
        when(client.getAsyncJob(JOB_ID)).thenReturn(asyncJob);
        expectedAsyncExecutionState = AsyncExecutionState.RUNNING;

        testExecuteOperations();

        verify(stepLogger).debug(eq(Messages.ASYNC_OPERATION_SERVICE_BINDING_IN_STATE_WITH_WARNINGS),
                                 eq(JOB_GUID), eq(JobState.PROCESSING), any());
    }

    @Test
    void testGetOnCompleteHandler() {
        prepareContext();
        CloudAsyncJob asyncJob = buildAsyncJobWithState(JobState.COMPLETE);
        when(client.getAsyncJob(JOB_ID)).thenReturn(asyncJob);
        expectedAsyncExecutionState = AsyncExecutionState.FINISHED;

        testExecuteOperations();

        verify(stepLogger).debug(Messages.ASYNC_OPERATION_FOR_SERVICE_BINDING_FINISHED, JOB_GUID);
    }

    @Test
    void testGetOnErrorHandlerWithManagedServiceInstance() {
        prepareContext();
        CloudServiceInstance serviceInstance = buildManagedServiceInstance();
        when(client.getServiceInstance(SERVICE_NAME, false)).thenReturn(serviceInstance);

        CloudAsyncJob asyncJob = buildAsyncJobWithError();
        when(client.getAsyncJob(JOB_ID)).thenReturn(asyncJob);
        expectedAsyncExecutionState = AsyncExecutionState.ERROR;

        testExecuteOperations();

        ArgumentCaptor<String> errorMessageCaptor = ArgumentCaptor.forClass(String.class);
        verify(stepLogger).error(errorMessageCaptor.capture());
        String errorMessage = errorMessageCaptor.getValue();
        assertTrue(errorMessage.contains(JOB_ID));
        assertTrue(errorMessage.contains(APP_NAME));
        assertTrue(errorMessage.contains(SERVICE_NAME));
        assertTrue(errorMessage.contains(SERVICE_OFFERING));
        assertTrue(errorMessage.contains(SERVICE_PLAN));
        assertTrue(errorMessage.contains(ERROR_MESSAGE));
    }

    @Test
    void testGetOnErrorHandlerWithUserProvidedServiceInstance() {
        prepareContext();
        CloudServiceInstance serviceInstance = buildUserProvidedServiceInstance();
        when(client.getServiceInstance(SERVICE_NAME, false)).thenReturn(serviceInstance);

        CloudAsyncJob asyncJob = buildAsyncJobWithError();
        when(client.getAsyncJob(JOB_ID)).thenReturn(asyncJob);
        expectedAsyncExecutionState = AsyncExecutionState.ERROR;

        testExecuteOperations();

        ArgumentCaptor<String> errorMessageCaptor = ArgumentCaptor.forClass(String.class);
        verify(stepLogger).error(errorMessageCaptor.capture());
        String errorMessage = errorMessageCaptor.getValue();
        assertTrue(errorMessage.contains(JOB_ID));
        assertTrue(errorMessage.contains(APP_NAME));
        assertTrue(errorMessage.contains(SERVICE_NAME));
        assertTrue(errorMessage.contains(ERROR_MESSAGE));
        assertTrue(!errorMessage.contains(SERVICE_OFFERING) || !errorMessage.contains(SERVICE_PLAN));
    }

    @Test
    void testGetOnErrorHandlerWithMissingServiceInstance() {
        prepareContext();
        when(client.getServiceInstance(SERVICE_NAME, false)).thenReturn(null);

        CloudAsyncJob asyncJob = buildAsyncJobWithError();
        when(client.getAsyncJob(JOB_ID)).thenReturn(asyncJob);
        expectedAsyncExecutionState = AsyncExecutionState.ERROR;

        testExecuteOperations();

        ArgumentCaptor<String> errorMessageCaptor = ArgumentCaptor.forClass(String.class);
        verify(stepLogger).error(errorMessageCaptor.capture());
        String errorMessage = errorMessageCaptor.getValue();
        assertTrue(errorMessage.contains(JOB_ID));
        assertTrue(errorMessage.contains(APP_NAME));
        assertTrue(errorMessage.contains(SERVICE_NAME));
        assertTrue(errorMessage.contains(ERROR_MESSAGE));
        assertTrue(errorMessage.contains("Instance not found"));
    }

    @Test
    void testGetOnErrorHandlerWithMissingServiceOfferingAndPlan() {
        prepareContext();
        CloudServiceInstance serviceInstance = buildManagedServiceInstanceWithNullFields();
        when(client.getServiceInstance(SERVICE_NAME, false)).thenReturn(serviceInstance);

        CloudAsyncJob asyncJob = buildAsyncJobWithError();
        when(client.getAsyncJob(JOB_ID)).thenReturn(asyncJob);
        expectedAsyncExecutionState = AsyncExecutionState.ERROR;

        testExecuteOperations();

        ArgumentCaptor<String> errorMessageCaptor = ArgumentCaptor.forClass(String.class);
        verify(stepLogger).error(errorMessageCaptor.capture());
        String errorMessage = errorMessageCaptor.getValue();
        assertTrue(errorMessage.contains("missing"));
    }

    @Test
    void testGetOnErrorHandlerForOptionalResource() {
        prepareContextWithOptionalService();
        CloudServiceInstance serviceInstance = buildManagedServiceInstance();
        when(client.getServiceInstance(SERVICE_NAME, false)).thenReturn(serviceInstance);

        CloudAsyncJob asyncJob = buildAsyncJobWithError();
        when(client.getAsyncJob(JOB_ID)).thenReturn(asyncJob);
        expectedAsyncExecutionState = AsyncExecutionState.FINISHED;

        testExecuteOperations();

        verify(stepLogger).warnWithoutProgressMessage(eq(Messages.ASYNC_OPERATION_FOR_SERVICE_BINDING_FOR_OPTIONAL_SERVICE_FAILED_WITH),
                                                      eq(JOB_GUID), eq(APP_NAME), eq(SERVICE_NAME), eq(ERROR_MESSAGE));
        verify(stepLogger, never()).error(anyString());
    }

    @Test
    void testGetOnErrorHandlerWithServiceToDelete() {
        prepareContextWithServiceToDelete();
        CloudServiceInstance serviceInstance = buildManagedServiceInstance();
        when(client.getServiceInstance(SERVICE_NAME, false)).thenReturn(serviceInstance);

        CloudAsyncJob asyncJob = buildAsyncJobWithError();
        when(client.getAsyncJob(JOB_ID)).thenReturn(asyncJob);
        expectedAsyncExecutionState = AsyncExecutionState.ERROR;

        testExecuteOperations();

        ArgumentCaptor<String> errorMessageCaptor = ArgumentCaptor.forClass(String.class);
        verify(stepLogger).error(errorMessageCaptor.capture());
        String errorMessage = errorMessageCaptor.getValue();
        assertTrue(errorMessage.contains(SERVICE_NAME));
    }

    private void prepareContext() {
        context.setVariable(Variables.SERVICE_UNBINDING_JOB_ID, JOB_ID);
        context.setVariable(Variables.APP_TO_PROCESS, ImmutableCloudApplicationExtended.builder()
                                                                                       .name(APP_NAME)
                                                                                       .metadata(ImmutableCloudMetadata.builder()
                                                                                                                       .guid(
                                                                                                                           UUID.randomUUID())
                                                                                                                       .build())
                                                                                       .build());
        context.setVariable(Variables.SERVICE_TO_UNBIND_BIND, SERVICE_NAME);
        context.setVariable(Variables.SERVICES_TO_BIND, List.of());
    }

    private void prepareContextWithOptionalService() {
        prepareContext();
        CloudServiceInstanceExtended optionalService = ImmutableCloudServiceInstanceExtended.builder()
                                                                                            .name(SERVICE_NAME)
                                                                                            .isOptional(true)
                                                                                            .metadata(ImmutableCloudMetadata.builder()
                                                                                                                            .guid(
                                                                                                                                UUID.randomUUID())
                                                                                                                            .build())
                                                                                            .build();
        context.setVariable(Variables.SERVICES_TO_BIND, List.of(optionalService));
    }

    private void prepareContextWithServiceToDelete() {
        context.setVariable(Variables.SERVICE_UNBINDING_JOB_ID, JOB_ID);
        context.setVariable(Variables.APP_TO_PROCESS, ImmutableCloudApplicationExtended.builder()
                                                                                       .name(APP_NAME)
                                                                                       .metadata(ImmutableCloudMetadata.builder()
                                                                                                                       .guid(
                                                                                                                           UUID.randomUUID())
                                                                                                                       .build())
                                                                                       .build());
        context.setVariable(Variables.SERVICE_TO_DELETE, SERVICE_NAME);
        context.setVariable(Variables.SERVICES_TO_BIND, List.of());
    }

    private CloudAsyncJob buildAsyncJobWithState(JobState state) {
        return ImmutableCloudAsyncJob.builder()
                                     .state(state)
                                     .metadata(ImmutableCloudMetadata.builder()
                                                                     .guid(JOB_GUID)
                                                                     .build())
                                     .build();
    }

    private CloudAsyncJob buildAsyncJobWithError() {
        return ImmutableCloudAsyncJob.builder()
                                     .state(JobState.FAILED)
                                     .metadata(ImmutableCloudMetadata.builder()
                                                                     .guid(JOB_GUID)
                                                                     .build())
                                     .errors(ERROR_MESSAGE)
                                     .build();
    }

    private CloudServiceInstance buildManagedServiceInstance() {
        return ImmutableCloudServiceInstance.builder()
                                            .name(SERVICE_NAME)
                                            .label(SERVICE_OFFERING)
                                            .plan(SERVICE_PLAN)
                                            .type(ServiceInstanceType.MANAGED)
                                            .metadata(ImmutableCloudMetadata.builder()
                                                                            .guid(UUID.randomUUID())
                                                                            .build())
                                            .build();
    }

    private CloudServiceInstance buildUserProvidedServiceInstance() {
        return ImmutableCloudServiceInstance.builder()
                                            .name(SERVICE_NAME)
                                            .type(ServiceInstanceType.USER_PROVIDED)
                                            .metadata(ImmutableCloudMetadata.builder()
                                                                            .guid(UUID.randomUUID())
                                                                            .build())
                                            .build();
    }

    private CloudServiceInstance buildManagedServiceInstanceWithNullFields() {
        return ImmutableCloudServiceInstance.builder()
                                            .name(SERVICE_NAME)
                                            .type(ServiceInstanceType.MANAGED)
                                            .metadata(ImmutableCloudMetadata.builder()
                                                                            .guid(UUID.randomUUID())
                                                                            .build())
                                            .build();
    }

    @Override
    protected List<AsyncExecution> getAsyncOperations(ProcessContext wrapper) {
        return List.of(new PollServiceUnbindingOperationExecution());
    }

    @Override
    protected void validateOperationExecutionResult(AsyncExecutionState result) {
        assertEquals(expectedAsyncExecutionState, result);
    }

    @Override
    protected UnbindServiceFromApplicationStep createStep() {
        return new UnbindServiceFromApplicationStep();
    }
}
