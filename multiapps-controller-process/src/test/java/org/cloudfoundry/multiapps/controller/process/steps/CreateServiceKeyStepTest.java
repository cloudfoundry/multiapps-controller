package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceInstance;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ImmutableServiceCredentialBindingOperation;
import com.sap.cloudfoundry.client.facade.domain.ServiceCredentialBindingOperation;

class CreateServiceKeyStepTest extends SyncFlowableStepTest<CreateServiceKeyStep> {

    private static final String JOB_ID = "123";
    private static final String SERVICE_KEY_NAME = "test_key_name";
    private static final String SERVICE_INSTANCE_NAME = "test_service_instance";
    private static final UUID SERVICE_KEY_GUID = UUID.randomUUID();

    @Test
    void testSyncServiceKeyCreation() {
        CloudServiceKey testKey = prepareServiceKey(buildCloudServiceInstanceExtended(false));
        step.execute(execution);
        assertStepFinishedSuccessfully();
        verify(client).createServiceKey(Mockito.eq(testKey), Mockito.eq(SERVICE_INSTANCE_NAME));
    }

    @Test
    void testAsyncServiceKeyCreation() {
        CloudServiceKey testKey = prepareServiceKey(buildCloudServiceInstanceExtended(false));
        when(client.createServiceKey(any(), any())).thenReturn(Optional.of(JOB_ID));
        step.execute(execution);
        assertEquals(StepPhase.POLL.toString(), getExecutionStatus());
        verify(client).createServiceKey(Mockito.eq(testKey), Mockito.eq(SERVICE_INSTANCE_NAME));
    }

    @Test
    void testThrowUnprocessableEntityCloudException() {
        CloudServiceInstanceExtended serviceInstance = buildCloudServiceInstanceExtended(false);
        CloudServiceKey testKey = prepareServiceKey(serviceInstance);
        when(client.createServiceKey(any(), any())).thenThrow(new CloudOperationException(HttpStatus.UNPROCESSABLE_ENTITY));
        context.setVariable(Variables.SERVICE_TO_PROCESS, serviceInstance);
        assertThrows(SLException.class, () -> step.execute(execution));
        verify(client).createServiceKey(Mockito.eq(testKey), Mockito.eq(SERVICE_INSTANCE_NAME));
    }

    @Test
    void testThrowCloudExceptionWithOptionalService() {
        CloudServiceInstanceExtended serviceInstance = buildCloudServiceInstanceExtended(true);
        CloudServiceKey testKey = prepareServiceKey(serviceInstance);
        when(client.createServiceKey(any(), any())).thenThrow(new CloudOperationException(HttpStatus.SERVICE_UNAVAILABLE));
        context.setVariable(Variables.SERVICE_TO_PROCESS, serviceInstance);
        step.execute(execution);
        assertStepFinishedSuccessfully();
        verify(client).createServiceKey(Mockito.eq(testKey), Mockito.eq(SERVICE_INSTANCE_NAME));
    }

    @Test
    void testThrowUnprocessableEntityExceptionWhenTheServiceKeyIsCreated() {
        CloudServiceInstanceExtended serviceInstance = buildCloudServiceInstanceExtended(false);
        CloudServiceKey testKey = buildCloudServiceKey(serviceInstance);
        context.setVariable(Variables.SERVICE_KEY_TO_PROCESS, testKey);
        when(client.createServiceKey(any(), any())).thenThrow(new CloudOperationException(HttpStatus.UNPROCESSABLE_ENTITY));
        when(client.getServiceKey(any(), any())).thenReturn(testKey);
        step.execute(execution);
        assertTrue(context.getVariable(Variables.USE_LAST_OPERATION_FOR_SERVICE_KEY_CREATION));
        assertEquals(StepPhase.POLL.toString(), getExecutionStatus());
        verify(client).createServiceKey(Mockito.eq(testKey), Mockito.eq(SERVICE_INSTANCE_NAME));
    }

    private CloudServiceKey prepareServiceKey(CloudServiceInstanceExtended serviceInstance) {
        CloudServiceKey serviceKeyToProcess = buildCloudServiceKey(serviceInstance);
        context.setVariable(Variables.SERVICE_KEY_TO_PROCESS, serviceKeyToProcess);
        return serviceKeyToProcess;
    }

    private CloudServiceKey buildCloudServiceKey(CloudServiceInstance serviceInstance) {
        return ImmutableCloudServiceKey.builder()
                                       .serviceInstance(ImmutableCloudServiceInstance.copyOf(serviceInstance))
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
    protected CreateServiceKeyStep createStep() {
        return new CreateServiceKeyStep();
    }
}
