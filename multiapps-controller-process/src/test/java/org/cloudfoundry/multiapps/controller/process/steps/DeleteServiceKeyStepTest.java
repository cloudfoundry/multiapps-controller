package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.MessageFormat;
import java.util.Optional;
import java.util.UUID;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ImmutableServiceCredentialBindingOperation;
import com.sap.cloudfoundry.client.facade.domain.ServiceCredentialBindingOperation;

class DeleteServiceKeyStepTest extends SyncFlowableStepTest<DeleteServiceKeyStep> {

    private static final String JOB_ID = "123";
    private static final String SERVICE_KEY_NAME = "test_key_name";
    private static final String SERVICE_INSTANCE_NAME = "test_service_instance";
    private static final UUID SERVICE_KEY_GUID = UUID.randomUUID();

    @Test
    void testSyncServiceKeyDeletion() {
        prepareServiceKey(buildCloudServiceInstanceExtended(false));
        step.execute(execution);
        assertStepFinishedSuccessfully();
        verify(client).deleteServiceBinding(any());
    }

    @Test
    void testAsyncServiceKeyDeletion() {
        prepareServiceKey(buildCloudServiceInstanceExtended(false));
        when(client.deleteServiceBinding(any())).thenReturn(Optional.of(JOB_ID));
        step.execute(execution);
        assertEquals(StepPhase.POLL.toString(), getExecutionStatus());
        verify(client).deleteServiceBinding(any());
    }

    @Test
    void testThrowUnprocessableEntityCloudException() {
        CloudServiceInstanceExtended serviceInstance = buildCloudServiceInstanceExtended(false);
        prepareServiceKey(serviceInstance);
        when(client.deleteServiceBinding(any())).thenThrow(new CloudOperationException(HttpStatus.UNPROCESSABLE_ENTITY));
        context.setVariable(Variables.SERVICE_TO_PROCESS, serviceInstance);
        assertThrows(SLException.class, () -> step.execute(execution));
        verify(client).deleteServiceBinding(any());
    }

    @Test
    void testThrowCloudExceptionWithOptionalService() {
        CloudServiceInstanceExtended serviceInstance = buildCloudServiceInstanceExtended(true);
        prepareServiceKey(serviceInstance);
        when(client.deleteServiceBinding(any())).thenThrow(new CloudOperationException(HttpStatus.SERVICE_UNAVAILABLE));
        context.setVariable(Variables.SERVICE_TO_PROCESS, serviceInstance);
        step.execute(execution);
        assertStepFinishedSuccessfully();
        verify(client).deleteServiceBinding(any());
    }

    @Test
    void testThrowUnprocessableEntityExceptionWhenTheServiceKeyIsDeleted() {
        CloudServiceInstanceExtended serviceInstance = buildCloudServiceInstanceExtended(false);
        CloudServiceKey serviceKeyToProcess = buildCloudServiceKey(serviceInstance);
        context.setVariable(Variables.SERVICE_KEY_TO_PROCESS, serviceKeyToProcess);
        when(client.deleteServiceBinding(any())).thenThrow(new CloudOperationException(HttpStatus.UNPROCESSABLE_ENTITY));
        when(client.getServiceKey(any(), any())).thenReturn(serviceKeyToProcess);
        step.execute(execution);
        assertTrue(context.getVariable(Variables.USE_LAST_OPERATION_FOR_SERVICE_KEY_DELETION));
        assertEquals(StepPhase.POLL.toString(), getExecutionStatus());
        verify(client).deleteServiceBinding(any());
    }

    @Test
    void testThrowNotFoundExceptionWhenDeleting() {
        CloudServiceInstanceExtended serviceInstance = buildCloudServiceInstanceExtended(false);
        prepareServiceKey(serviceInstance);
        when(client.deleteServiceBinding(any())).thenThrow(new CloudOperationException(HttpStatus.NOT_FOUND));
        step.execute(execution);
        assertStepFinishedSuccessfully();
        verify(client).deleteServiceBinding(any());
    }

    @Test
    void testThrowInternalErrorExceptionWhenDeleting() {
        CloudServiceInstanceExtended serviceInstance = buildCloudServiceInstanceExtended(false);
        prepareServiceKey(serviceInstance);
        when(client.deleteServiceBinding(any())).thenThrow(new CloudOperationException(HttpStatus.INTERNAL_SERVER_ERROR));
        Exception exception = assertThrows(SLException.class, () -> step.execute(execution));
        assertTrue(exception.getMessage()
                            .contains(MessageFormat.format(Messages.ERROR_OCCURRED_WHILE_DELETING_SERVICE_KEY_0, SERVICE_KEY_NAME)));
    }

    private void prepareServiceKey(CloudServiceInstanceExtended serviceInstance) {
        CloudServiceKey serviceKeyToProcess = buildCloudServiceKey(serviceInstance);
        context.setVariable(Variables.SERVICE_KEY_TO_PROCESS, serviceKeyToProcess);
    }

    private CloudServiceKey buildCloudServiceKey(CloudServiceInstanceExtended serviceInstance) {
        return ImmutableCloudServiceKey.builder()
                                       .serviceInstance(serviceInstance)
                                       .name(SERVICE_KEY_NAME)
                                       .metadata(ImmutableCloudMetadata.of(SERVICE_KEY_GUID))
                                       .serviceKeyOperation(ImmutableServiceCredentialBindingOperation.builder()
                                                                                                      .type(ServiceCredentialBindingOperation.Type.CREATE)
                                                                                                      .state(ServiceCredentialBindingOperation.State.SUCCEEDED)
                                                                                                      .build())
                                       .build();
    }

    private CloudServiceInstanceExtended buildCloudServiceInstanceExtended(boolean isOptional) {
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .name(SERVICE_INSTANCE_NAME)
                                                    .isOptional(isOptional)
                                                    .build();
    }

    @Override
    protected DeleteServiceKeyStep createStep() {
        return new DeleteServiceKeyStep();
    }
}
