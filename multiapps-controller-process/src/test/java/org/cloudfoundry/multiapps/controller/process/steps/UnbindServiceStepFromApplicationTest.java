package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.util.DeletingServiceBindingOperationCallback;
import org.cloudfoundry.multiapps.controller.process.util.UnbindServiceFromApplicationCallback;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.ApplicationServicesUpdateCallback;
import com.sap.cloudfoundry.client.facade.CloudControllerException;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBinding;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceBinding;
import com.sap.cloudfoundry.client.facade.domain.ImmutableServiceCredentialBindingOperation;
import com.sap.cloudfoundry.client.facade.domain.ServiceCredentialBindingOperation;

class UnbindServiceStepFromApplicationTest extends SyncFlowableStepTest<UnbindServiceFromApplicationStep> {

    private static final String APPLICATION_NAME = "test_application";
    private static final String SERVICE_NAME = "test_service";
    private static final String JOB_ID = "123";
    private static final UUID SERVICE_INSTANCE_GUID = UUID.randomUUID();
    private static final UUID APP_GUID = UUID.randomUUID();
    private static final UUID SERVICE_BINDING_GUID = UUID.randomUUID();

    @Test
    void testAsyncUnbindService() {
        prepareContext();
        when(client.unbindServiceInstance(eq(APPLICATION_NAME), eq(SERVICE_NAME),
                                          any(ApplicationServicesUpdateCallback.class))).thenReturn(Optional.of(JOB_ID));
        step.execute(execution);
        assertEquals(StepPhase.POLL.toString(), getExecutionStatus());
        verify(client).unbindServiceInstance(eq(APPLICATION_NAME), eq(SERVICE_NAME), any(ApplicationServicesUpdateCallback.class));
    }

    @Test
    void testSyncUnbindService() {
        prepareContext();
        step.execute(execution);
        assertStepFinishedSuccessfully();
        verify(client).unbindServiceInstance(eq(APPLICATION_NAME), eq(SERVICE_NAME), any(ApplicationServicesUpdateCallback.class));
    }

    @Test
    void testAsyncServiceUnbindingWithPollingByLastOperation() {
        prepareContext();
        when(client.unbindServiceInstance(eq(APPLICATION_NAME), eq(SERVICE_NAME),
                                          any(ApplicationServicesUpdateCallback.class))).then(answer -> {
                                              context.setVariable(Variables.USE_LAST_OPERATION_FOR_SERVICE_BINDING_DELETION, true);
                                              return Optional.empty();
                                          });
        step.execute(execution);
        assertEquals(StepPhase.POLL.toString(), getExecutionStatus());
        verify(client).unbindServiceInstance(eq(APPLICATION_NAME), eq(SERVICE_NAME), any(ApplicationServicesUpdateCallback.class));
        assertTrue(context.getVariable(Variables.USE_LAST_OPERATION_FOR_SERVICE_BINDING_DELETION));
    }

    @Test
    void testDeleteServiceBinding() {
        context.setVariable(Variables.SERVICE_BINDING_TO_DELETE, buildCloudServiceBinding());
        step.execute(execution);
        assertStepFinishedSuccessfully();
        verify(client).deleteServiceBinding(eq(SERVICE_BINDING_GUID), any(DeletingServiceBindingOperationCallback.class));
    }

    @Test
    void testDoNotThrowExceptionWhenServiceBindingAlreadyDeletedByApplicationCallback() {
        ProcessContext customProcessContext = new ProcessContext(execution, stepLogger, clientProvider);
        assertDoesNotThrow(() -> handleErrorInApplicationCallback(customProcessContext, HttpStatus.NOT_FOUND));
    }

    @Test
    void testDoNotThrowExceptionOnOptionalServiceWhenServerErrorApplicationCallback() {
        ProcessContext customProcessContext = new ProcessContext(execution, stepLogger, clientProvider);
        customProcessContext.setVariable(Variables.SERVICES_TO_BIND, List.of(ImmutableCloudServiceInstanceExtended.builder()
                                                                                                                  .name(SERVICE_NAME)
                                                                                                                  .isOptional(true)
                                                                                                                  .build()));
        assertDoesNotThrow(() -> handleErrorInApplicationCallback(customProcessContext, HttpStatus.BAD_GATEWAY));
    }

    @Test
    void testThrowExceptionOnNotOptionalServiceWhenServerError() {
        ProcessContext customProcessContext = new ProcessContext(execution, stepLogger, clientProvider);
        customProcessContext.setVariable(Variables.SERVICES_TO_BIND, List.of(ImmutableCloudServiceInstanceExtended.builder()
                                                                                                                  .name(SERVICE_NAME)
                                                                                                                  .isOptional(false)
                                                                                                                  .build()));
        assertThrows(SLException.class, () -> handleErrorInApplicationCallback(customProcessContext, HttpStatus.BAD_GATEWAY));
    }

    @Test
    void testGetStepErrorMessageWhenServiceBindingToDeleteIsSet() {
        context.setVariable(Variables.SERVICE_BINDING_TO_DELETE, buildCloudServiceBinding());
        assertEquals(MessageFormat.format("Error while deleting service binding \"{0}\"", SERVICE_BINDING_GUID),
                     step.getStepErrorMessage(context));
    }

    @Test
    void testGetStepErrorMessageWhenServiceBindingToDeleteIsNotSet() {
        prepareContext();
        assertEquals("Error while unbinding service instance \"test_service\" from application \"test_application\"",
                     step.getStepErrorMessage(context));
    }

    @Test
    void testDoNotThrowExceptionWhenServiceBindingAlreadyDeletedByOperationCallback() {
        ProcessContext customProcessContext = new ProcessContext(execution, stepLogger, clientProvider);
        assertDoesNotThrow(() -> handleErrorInOperationCallback(customProcessContext, HttpStatus.NOT_FOUND));
    }

    @Test
    void testDoNotThrowExceptionWhenDeletionIsInProgressByApplicationCallback() {
        ProcessContext customProcessContext = new ProcessContext(execution, stepLogger, clientProvider);
        CloudServiceBinding serviceBinding = buildCloudServiceBinding();
        when(client.getServiceBindingForApplication(any(), any())).thenReturn(serviceBinding);
        assertDoesNotThrow(() -> handleErrorInApplicationCallback(customProcessContext, HttpStatus.UNPROCESSABLE_ENTITY));
        assertTrue(customProcessContext.getVariable(Variables.USE_LAST_OPERATION_FOR_SERVICE_BINDING_DELETION));
    }

    @Test
    void testDoNotThrowExceptionWhenDeletionIsInProgressByOperationCallback() {
        ProcessContext customProcessContext = new ProcessContext(execution, stepLogger, clientProvider);
        CloudServiceBinding serviceBinding = buildCloudServiceBinding();
        when(client.getServiceBinding(SERVICE_BINDING_GUID)).thenReturn(serviceBinding);
        assertDoesNotThrow(() -> handleErrorInOperationCallback(customProcessContext, HttpStatus.UNPROCESSABLE_ENTITY));
        assertTrue(customProcessContext.getVariable(Variables.USE_LAST_OPERATION_FOR_SERVICE_BINDING_DELETION));
    }

    @Test
    void testThrowExceptionOnNotOptionalServiceWhenServerErrorOperationCallback() {
        ProcessContext customProcessContext = new ProcessContext(execution, stepLogger, clientProvider);
        Exception exception = assertThrows(SLException.class,
                                           () -> handleErrorInOperationCallback(customProcessContext, HttpStatus.INTERNAL_SERVER_ERROR));
        assertEquals(MessageFormat.format("Error occurred while deleting service binding \"{0}\"", SERVICE_BINDING_GUID),
                     exception.getMessage());
    }

    private void handleErrorInApplicationCallback(ProcessContext customProcessContext, HttpStatus httpStatus) {
        new UnbindServiceFromApplicationCallback(customProcessContext, client).onError(new CloudOperationException(httpStatus),
                                                                                       APPLICATION_NAME, SERVICE_NAME);
    }

    private void handleErrorInOperationCallback(ProcessContext context, HttpStatus httpStatus) {
        new DeletingServiceBindingOperationCallback(context, client).onError(new CloudControllerException(httpStatus),
                                                                             SERVICE_BINDING_GUID);
    }

    private void prepareContext() {
        CloudApplicationExtended application = ImmutableCloudApplicationExtended.builder()
                                                                                .name(APPLICATION_NAME)
                                                                                .build();
        context.setVariable(Variables.APP_TO_PROCESS, application);
        context.setVariable(Variables.SERVICE_TO_UNBIND_BIND, SERVICE_NAME);
    }

    private CloudServiceBinding buildCloudServiceBinding() {
        return ImmutableCloudServiceBinding.builder()
                                           .metadata(ImmutableCloudMetadata.of(SERVICE_BINDING_GUID))
                                           .serviceInstanceGuid(SERVICE_INSTANCE_GUID)
                                           .applicationGuid(APP_GUID)
                                           .serviceBindingOperation(ImmutableServiceCredentialBindingOperation.builder()
                                                                                                              .type(ServiceCredentialBindingOperation.Type.CREATE)
                                                                                                              .state(ServiceCredentialBindingOperation.State.IN_PROGRESS)
                                                                                                              .createdAt(LocalDateTime.now())
                                                                                                              .updatedAt(LocalDateTime.now())
                                                                                                              .build())
                                           .build();
    }

    @Override
    protected UnbindServiceFromApplicationStep createStep() {
        return new UnbindServiceFromApplicationStep();
    }

}
