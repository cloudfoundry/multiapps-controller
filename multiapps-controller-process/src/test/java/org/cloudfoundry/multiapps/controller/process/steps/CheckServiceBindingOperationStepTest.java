package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBinding;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceBinding;
import com.sap.cloudfoundry.client.facade.domain.ImmutableServiceCredentialBindingOperation;
import com.sap.cloudfoundry.client.facade.domain.ServiceCredentialBindingOperation;

class CheckServiceBindingOperationStepTest extends SyncFlowableStepTest<CheckServiceBindingOperationStep> {

    private static final String APP_NAME = "test-app";
    private static final UUID APP_GUID = UUID.randomUUID();
    private static final String SERVICE_INSTANCE_NAME = "test-service-instance";
    private static final UUID SERVICE_INSTANCE_GUID = UUID.randomUUID();
    private static final UUID SERVICE_BINDING_GUID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        prepareContextVariables();
    }

    @Test
    void testCheckBindingWhenBindingDoesNotExist() {
        step.execute(execution);
        assertStepFinishedSuccessfully();
    }

    @Test
    void testCheckServiceBindingWhenServiceBindingToDeleteIsSet() {
        CloudServiceBinding serviceBindingToDelete = buildCloudServiceBinding(ServiceCredentialBindingOperation.State.INITIAL);
        context.setVariable(Variables.SERVICE_BINDING_TO_DELETE, serviceBindingToDelete);
        step.execute(execution);
        assertEquals(StepPhase.POLL.toString(), getExecutionStatus());
    }

    static Stream<Arguments> testCheckServiceBindingWhichIsInDifferentStates() {
        return Stream.of(Arguments.of(ServiceCredentialBindingOperation.State.INITIAL, StepPhase.POLL),
                         Arguments.of(ServiceCredentialBindingOperation.State.IN_PROGRESS, StepPhase.POLL),
                         Arguments.of(ServiceCredentialBindingOperation.State.SUCCEEDED, StepPhase.DONE),
                         Arguments.of(ServiceCredentialBindingOperation.State.FAILED, StepPhase.DONE));
    }

    @MethodSource
    @ParameterizedTest
    void testCheckServiceBindingWhichIsInDifferentStates(ServiceCredentialBindingOperation.State bindingState,
                                                         StepPhase expectedStepPhase) {
        CloudServiceBinding serviceBinding = buildCloudServiceBinding(bindingState);
        when(client.getServiceBindingForApplication(APP_GUID, SERVICE_INSTANCE_GUID)).thenReturn(serviceBinding);
        when(client.getApplicationGuid(APP_NAME)).thenReturn(APP_GUID);
        when(client.getRequiredServiceInstanceGuid(SERVICE_INSTANCE_NAME)).thenReturn(SERVICE_INSTANCE_GUID);
        step.execute(execution);
        assertEquals(expectedStepPhase.toString(), getExecutionStatus());
    }

    @Test
    void testThrowExceptionWhenFetchingServiceBinding() {
        when(client.getServiceBindingForApplication(APP_GUID,
                                                    SERVICE_INSTANCE_GUID)).thenThrow(new CloudOperationException(HttpStatus.SERVICE_UNAVAILABLE));
        when(client.getApplicationGuid(APP_NAME)).thenReturn(APP_GUID);
        when(client.getRequiredServiceInstanceGuid(SERVICE_INSTANCE_NAME)).thenReturn(SERVICE_INSTANCE_GUID);
        Exception exception = assertThrows(SLException.class, () -> step.execute(execution));
        assertEquals("Error while checking service binding operations between app: \"test-app\" and service instance \"test-service-instance\": Controller operation failed: 503 Service Unavailable",
                     exception.getMessage()
                              .trim());
    }

    @Test
    void testThrowExceptionWhenFetchingServiceBindingWithOptionalService() {
        when(client.getServiceBindingForApplication(APP_GUID,
                                                    SERVICE_INSTANCE_GUID)).thenThrow(new CloudOperationException(HttpStatus.BAD_GATEWAY));
        CloudServiceInstanceExtended optionalServiceInstance = buildOptionalCloudServiceInstanceExtended();
        context.setVariable(Variables.SERVICES_TO_BIND, List.of(optionalServiceInstance));
        when(client.getApplicationGuid(APP_NAME)).thenReturn(APP_GUID);
        when(client.getRequiredServiceInstanceGuid(SERVICE_INSTANCE_NAME)).thenReturn(SERVICE_INSTANCE_GUID);
        step.execute(execution);
        assertStepFinishedSuccessfully();
    }

    @Test
    void testGetStepErrorMessageDuringServiceBindingDeletion() {
        CloudServiceBinding serviceBindingToDelete = buildCloudServiceBinding(ServiceCredentialBindingOperation.State.SUCCEEDED);
        context.setVariable(Variables.SERVICE_BINDING_TO_DELETE, serviceBindingToDelete);
        assertEquals(MessageFormat.format(Messages.ERROR_WHILE_CHECKING_SERVICE_BINDING_OPERATIONS_0, SERVICE_BINDING_GUID),
                     step.getStepErrorMessage(context));
    }

    @Test
    void testGetStepErrorMessageDuringServiceBindingCreation() {
        CloudApplicationExtended app = buildCloudApplicationExtended();
        context.setVariable(Variables.APP_TO_PROCESS, app);
        context.setVariable(Variables.SERVICE_TO_UNBIND_BIND, SERVICE_INSTANCE_NAME);
        assertEquals(MessageFormat.format(Messages.ERROR_WHILE_CHECKING_SERVICE_BINDING_OPERATIONS_BETWEEN_APP_0_AND_SERVICE_INSTANCE_1,
                                          APP_NAME, SERVICE_INSTANCE_NAME),
                     step.getStepErrorMessage(context));
    }

    private void prepareContextVariables() {
        CloudApplicationExtended appToProcess = buildCloudApplicationExtended();
        context.setVariable(Variables.APP_TO_PROCESS, appToProcess);
        context.setVariable(Variables.SERVICE_TO_UNBIND_BIND, SERVICE_INSTANCE_NAME);
    }

    private CloudApplicationExtended buildCloudApplicationExtended() {
        return ImmutableCloudApplicationExtended.builder()
                                                .name(APP_NAME)
                                                .metadata(ImmutableCloudMetadata.of(APP_GUID))
                                                .build();
    }

    private CloudServiceBinding buildCloudServiceBinding(ServiceCredentialBindingOperation.State bindingState) {
        return ImmutableCloudServiceBinding.builder()
                                           .metadata(ImmutableCloudMetadata.of(SERVICE_BINDING_GUID))
                                           .serviceInstanceGuid(SERVICE_INSTANCE_GUID)
                                           .applicationGuid(APP_GUID)
                                           .serviceBindingOperation(ImmutableServiceCredentialBindingOperation.builder()
                                                                                                              .type(ServiceCredentialBindingOperation.Type.DELETE)
                                                                                                              .state(bindingState)
                                                                                                              .build())
                                           .build();
    }

    private CloudServiceInstanceExtended buildOptionalCloudServiceInstanceExtended() {
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .isOptional(true)
                                                    .name(SERVICE_INSTANCE_NAME)
                                                    .metadata(ImmutableCloudMetadata.of(SERVICE_INSTANCE_GUID))
                                                    .build();
    }

    @Override
    protected CheckServiceBindingOperationStep createStep() {
        return new CheckServiceBindingOperationStep();
    }
}
