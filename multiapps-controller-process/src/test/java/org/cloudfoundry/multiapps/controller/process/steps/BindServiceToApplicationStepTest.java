package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.util.DefaultApplicationServicesUpdateCallback;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.ApplicationServicesUpdateCallback;
import com.sap.cloudfoundry.client.facade.CloudControllerException;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBinding;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceBinding;
import com.sap.cloudfoundry.client.facade.domain.ImmutableServiceCredentialBindingOperation;
import com.sap.cloudfoundry.client.facade.domain.ServiceCredentialBindingOperation;

class BindServiceToApplicationStepTest extends SyncFlowableStepTest<BindServiceToApplicationStep> {

    private static final String JOB_ID = "123";
    private static final String APPLICATION_NAME = "test_application";
    private static final String SERVICE_NAME = "test_service";
    private static final UUID SERVICE_INSTANCE_GUID = UUID.randomUUID();
    private static final UUID APP_GUID = UUID.randomUUID();
    private static final Map<String, Object> BINDING_PARAMETERS = Map.of("test-config", "test-value");

    @Test
    void testAsyncServiceBinding() {
        prepareContext();
        when(client.bindServiceInstance(eq(APPLICATION_NAME), eq(SERVICE_NAME), eq(BINDING_PARAMETERS),
                                        any(ApplicationServicesUpdateCallback.class))).thenReturn(Optional.of(JOB_ID));
        step.execute(execution);
        assertEquals(StepPhase.POLL.toString(), getExecutionStatus());
        verify(client).bindServiceInstance(eq(APPLICATION_NAME), eq(SERVICE_NAME), eq(BINDING_PARAMETERS),
                                           any(ApplicationServicesUpdateCallback.class));
        assertEquals(JOB_ID, context.getVariable(Variables.SERVICE_BINDING_JOB_ID));
    }

    @Test
    void testSyncServiceBinding() {
        prepareContext();
        step.execute(execution);
        assertStepFinishedSuccessfully();
        verify(client).bindServiceInstance(eq(APPLICATION_NAME), eq(SERVICE_NAME), eq(BINDING_PARAMETERS),
                                           any(ApplicationServicesUpdateCallback.class));
    }

    @Test
    void testAsyncServiceBindingWithPollingByLastOperation() {
        prepareContext();
        when(client.bindServiceInstance(eq(APPLICATION_NAME), eq(SERVICE_NAME), eq(BINDING_PARAMETERS),
                                        any(ApplicationServicesUpdateCallback.class))).then(answer -> {
                                            context.setVariable(Variables.USE_LAST_OPERATION_FOR_SERVICE_BINDING_CREATION, true);
                                            return Optional.empty();
                                        });
        step.execute(execution);
        assertEquals(StepPhase.POLL.toString(), getExecutionStatus());
        assertTrue(context.getVariable(Variables.USE_LAST_OPERATION_FOR_SERVICE_BINDING_CREATION));
    }

    private void prepareContext() {
        CloudApplicationExtended application = ImmutableCloudApplicationExtended.builder()
                                                                                .name(APPLICATION_NAME)
                                                                                .build();
        context.setVariable(Variables.APP_TO_PROCESS, application);
        context.setVariable(Variables.SERVICE_TO_UNBIND_BIND, SERVICE_NAME);
        context.setVariable(Variables.SERVICE_BINDING_PARAMETERS, BINDING_PARAMETERS);
    }

    @Test
    void testDoNotThrowExceptionWhenServiceAlreadyBound() {
        ProcessContext customProcessContext = new ProcessContext(execution, stepLogger, clientProvider);
        customProcessContext.setVariable(Variables.SERVICES_TO_BIND, List.of(ImmutableCloudServiceInstanceExtended.builder()
                                                                                                                  .name(SERVICE_NAME)
                                                                                                                  .build()));
        assertDoesNotThrow(() -> handleServiceAlreadyBoundErrorInCallback(customProcessContext));
        assertTrue(customProcessContext.getVariable(Variables.USE_LAST_OPERATION_FOR_SERVICE_BINDING_CREATION));
    }

    @Test
    void testDoNotThrowExceptionOnOptionalService() {
        ProcessContext customProcessContext = new ProcessContext(execution, stepLogger, clientProvider);
        customProcessContext.setVariable(Variables.SERVICES_TO_BIND, List.of(ImmutableCloudServiceInstanceExtended.builder()
                                                                                                                  .name(SERVICE_NAME)
                                                                                                                  .isOptional(true)
                                                                                                                  .build()));
        assertDoesNotThrow(() -> handleErrorInCallback(customProcessContext));
    }

    @Test
    void testThrowExceptionOnNotOptionalService() {
        ProcessContext customProcessContext = new ProcessContext(execution, stepLogger, clientProvider);
        customProcessContext.setVariable(Variables.SERVICES_TO_BIND, List.of(ImmutableCloudServiceInstanceExtended.builder()
                                                                                                                  .name(SERVICE_NAME)
                                                                                                                  .isOptional(false)
                                                                                                                  .build()));
        assertThrows(SLException.class, () -> handleErrorInCallback(customProcessContext));
    }

    private void handleErrorInCallback(ProcessContext customProcessContext) {
        new DefaultApplicationServicesUpdateCallback(customProcessContext,
                                                     client).onError(new CloudOperationException(HttpStatus.BAD_GATEWAY), APPLICATION_NAME,
                                                                     SERVICE_NAME);
    }

    private void handleServiceAlreadyBoundErrorInCallback(ProcessContext customProcessContext) {
        CloudOperationException cloudException = new CloudControllerException(HttpStatus.UNPROCESSABLE_ENTITY);
        when(client.getServiceBindingForApplication(any(), any())).thenReturn(buildCloudServiceBinding());
        new DefaultApplicationServicesUpdateCallback(customProcessContext, client).onError(cloudException, APPLICATION_NAME, SERVICE_NAME);
    }

    private CloudServiceBinding buildCloudServiceBinding() {
        return ImmutableCloudServiceBinding.builder()
                                           .serviceInstanceGuid(SERVICE_INSTANCE_GUID)
                                           .applicationGuid(APP_GUID)
                                           .serviceBindingOperation(buildServiceCredentialBindingOperation())
                                           .build();
    }

    private ServiceCredentialBindingOperation buildServiceCredentialBindingOperation() {
        return ImmutableServiceCredentialBindingOperation.builder()
                                                         .createdAt(LocalDateTime.now())
                                                         .updatedAt(LocalDateTime.now())
                                                         .type(ServiceCredentialBindingOperation.Type.CREATE)
                                                         .state(ServiceCredentialBindingOperation.State.IN_PROGRESS)
                                                         .build();
    }

    @Test
    void testGetStepErrorMessage() {
        prepareContext();
        assertEquals("Error while binding service instance \"test_service\" to application \"test_application\"",
                     step.getStepErrorMessage(context));
    }

    @Override
    protected BindServiceToApplicationStep createStep() {
        return new BindServiceToApplicationStep();
    }

}
