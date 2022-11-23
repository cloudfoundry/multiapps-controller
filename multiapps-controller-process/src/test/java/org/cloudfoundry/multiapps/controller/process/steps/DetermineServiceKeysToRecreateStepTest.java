package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ImmutableServiceCredentialBindingOperation;
import com.sap.cloudfoundry.client.facade.domain.ServiceCredentialBindingOperation;

class DetermineServiceKeysToRecreateStepTest extends SyncFlowableStepTest<DetermineServiceKeysToRecreateStep> {

    private static final String SERVICE_INSTANCE_NAME = "service-instance";
    private static final String SERVICE_KEY_NAME = "service-key";
    private static final UUID SERVICE_KEY_GUID = UUID.randomUUID();
    private static final String EXISTING_SERVICE_KEY_NAME = "service-key-existing";
    private static final UUID EXISTING_SERVICE_KEY_GUID = UUID.randomUUID();

    @Test
    void testGetServiceKeysForOptionalServiceWhichDoesNotExist() {
        CloudServiceInstanceExtended serviceInstance = buildCloudServiceInstanceExtended(true);
        CloudServiceKey serviceKey = buildCloudServiceKey();
        context.setVariable(Variables.SERVICE_TO_PROCESS, serviceInstance);
        context.setVariable(Variables.SERVICE_KEYS_TO_CREATE, Map.of(SERVICE_INSTANCE_NAME, List.of(serviceKey)));
        when(client.getServiceKeysWithCredentials(SERVICE_INSTANCE_NAME)).thenThrow(new CloudOperationException(HttpStatus.SERVICE_UNAVAILABLE));
        step.execute(execution);
        assertStepFinishedSuccessfully();
        assertEquals(Collections.emptyList(), context.getVariable(Variables.CLOUD_SERVICE_KEYS_TO_DELETE));
        assertEquals(Collections.emptyList(), context.getVariable(Variables.CLOUD_SERVICE_KEYS_TO_CREATE));
    }

    @Test
    void testCreateNewServiceKey() {
        CloudServiceInstanceExtended serviceInstance = buildCloudServiceInstanceExtended(false);
        CloudServiceKey serviceKeyToCreate = buildCloudServiceKey();
        context.setVariable(Variables.SERVICE_TO_PROCESS, serviceInstance);
        context.setVariable(Variables.SERVICE_KEYS_TO_CREATE, Map.of(SERVICE_INSTANCE_NAME, List.of(serviceKeyToCreate)));
        step.execute(execution);
        assertStepFinishedSuccessfully();
        assertEquals(List.of(serviceKeyToCreate), context.getVariable(Variables.CLOUD_SERVICE_KEYS_TO_CREATE));
        assertEquals(Collections.emptyList(), context.getVariable(Variables.CLOUD_SERVICE_KEYS_TO_DELETE));
    }

    @Test
    void testUpdateServiceKey() {
        CloudServiceInstanceExtended serviceInstance = buildCloudServiceInstanceExtended(false);
        CloudServiceKey serviceKeyToUpdate = buildCloudServiceKey();
        context.setVariable(Variables.SERVICE_TO_PROCESS, serviceInstance);
        context.setVariable(Variables.SERVICE_KEYS_TO_CREATE, Map.of(SERVICE_INSTANCE_NAME, List.of(serviceKeyToUpdate)));
        CloudServiceKey serviceKeyWithCredentials = buildCloudServiceKeyWithCredentialsInProgress();
        when(client.getServiceKeysWithCredentials(SERVICE_INSTANCE_NAME)).thenReturn(List.of(serviceKeyWithCredentials));
        context.setVariable(Variables.DELETE_SERVICE_KEYS, true);
        step.execute(execution);
        assertStepFinishedSuccessfully();
        assertEquals(List.of(serviceKeyToUpdate), context.getVariable(Variables.CLOUD_SERVICE_KEYS_TO_CREATE));
        assertEquals(List.of(serviceKeyToUpdate), context.getVariable(Variables.CLOUD_SERVICE_KEYS_TO_DELETE));
    }

    @Test
    void testUpdateServiceKeyWhichIsInProgress() {
        CloudServiceInstanceExtended serviceInstance = buildCloudServiceInstanceExtended(false);
        CloudServiceKey serviceKeyToUpdate = buildCloudServiceKey();
        context.setVariable(Variables.SERVICE_TO_PROCESS, serviceInstance);
        context.setVariable(Variables.SERVICE_KEYS_TO_CREATE, Map.of(SERVICE_INSTANCE_NAME, List.of(serviceKeyToUpdate)));
        CloudServiceKey serviceKeyWithCredentials = buildCloudServiceInProgress();
        when(client.getServiceKeysWithCredentials(SERVICE_INSTANCE_NAME)).thenReturn(List.of(serviceKeyWithCredentials));
        context.setVariable(Variables.DELETE_SERVICE_KEYS, true);
        step.execute(execution);
        assertStepFinishedSuccessfully();
        assertEquals(List.of(serviceKeyToUpdate), context.getVariable(Variables.CLOUD_SERVICE_KEYS_TO_CREATE));
        assertEquals(List.of(serviceKeyToUpdate), context.getVariable(Variables.CLOUD_SERVICE_KEYS_TO_DELETE));
    }

    @Test
    void testServiceKeyDeletion() {
        CloudServiceInstanceExtended serviceInstance = buildCloudServiceInstanceExtended(false);
        CloudServiceKey serviceKeyToCreate = buildCloudServiceKey();
        context.setVariable(Variables.SERVICE_TO_PROCESS, serviceInstance);
        context.setVariable(Variables.SERVICE_KEYS_TO_CREATE, Map.of(SERVICE_INSTANCE_NAME, List.of(serviceKeyToCreate)));
        CloudServiceKey existingServiceKey = buildExisingCloudServiceKey();
        when(client.getServiceKeysWithCredentials(SERVICE_INSTANCE_NAME)).thenReturn(List.of(existingServiceKey));
        context.setVariable(Variables.DELETE_SERVICE_KEYS, true);
        step.execute(execution);
        assertStepFinishedSuccessfully();
        assertEquals(List.of(serviceKeyToCreate), context.getVariable(Variables.CLOUD_SERVICE_KEYS_TO_CREATE));
        assertEquals(List.of(existingServiceKey), context.getVariable(Variables.CLOUD_SERVICE_KEYS_TO_DELETE));
    }

    @Test
    void testThrowExceptionWithNonOptionalService() {
        CloudServiceInstanceExtended serviceInstance = buildCloudServiceInstanceExtended(false);
        CloudServiceKey serviceKeyToCreate = buildCloudServiceKey();
        context.setVariable(Variables.SERVICE_TO_PROCESS, serviceInstance);
        context.setVariable(Variables.SERVICE_KEYS_TO_CREATE, Map.of(SERVICE_INSTANCE_NAME, List.of(serviceKeyToCreate)));
        when(client.getServiceKeysWithCredentials(SERVICE_INSTANCE_NAME)).thenThrow(new CloudOperationException(HttpStatus.SERVICE_UNAVAILABLE));
        Exception exception = assertThrows(SLException.class, () -> step.execute(execution));
        assertEquals("Error while determining service keys to recreate: Controller operation failed: 503 Service Unavailable ",
                     exception.getMessage());
    }

    private CloudServiceInstanceExtended buildCloudServiceInstanceExtended(boolean isOptional) {
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .name(SERVICE_INSTANCE_NAME)
                                                    .isOptional(isOptional)
                                                    .resourceName(SERVICE_INSTANCE_NAME)
                                                    .build();
    }

    private CloudServiceKey buildCloudServiceKey() {
        return ImmutableCloudServiceKey.builder()
                                       .name(SERVICE_KEY_NAME)
                                       .metadata(ImmutableCloudMetadata.of(SERVICE_KEY_GUID))
                                       .serviceKeyOperation(ImmutableServiceCredentialBindingOperation.builder()
                                                                                                      .type(ServiceCredentialBindingOperation.Type.CREATE)
                                                                                                      .state(ServiceCredentialBindingOperation.State.SUCCEEDED)
                                                                                                      .build())
                                       .build();
    }

    private CloudServiceKey buildCloudServiceKeyWithCredentialsInProgress() {
        return ImmutableCloudServiceKey.builder()
                                       .name(SERVICE_KEY_NAME)
                                       .metadata(ImmutableCloudMetadata.of(SERVICE_KEY_GUID))
                                       .putCredential("test-key", "test-value")
                                       .serviceKeyOperation(ImmutableServiceCredentialBindingOperation.builder()
                                                                                                      .type(ServiceCredentialBindingOperation.Type.CREATE)
                                                                                                      .state(ServiceCredentialBindingOperation.State.SUCCEEDED)
                                                                                                      .build())
                                       .build();
    }

    private CloudServiceKey buildCloudServiceInProgress() {
        return ImmutableCloudServiceKey.builder()
                                       .name(SERVICE_KEY_NAME)
                                       .metadata(ImmutableCloudMetadata.of(SERVICE_KEY_GUID))
                                       .serviceKeyOperation(ImmutableServiceCredentialBindingOperation.builder()
                                                                                                      .type(ServiceCredentialBindingOperation.Type.CREATE)
                                                                                                      .state(ServiceCredentialBindingOperation.State.IN_PROGRESS)
                                                                                                      .build())
                                       .build();
    }

    private CloudServiceKey buildExisingCloudServiceKey() {
        return ImmutableCloudServiceKey.builder()
                                       .name(EXISTING_SERVICE_KEY_NAME)
                                       .metadata(ImmutableCloudMetadata.of(EXISTING_SERVICE_KEY_GUID))
                                       .serviceKeyOperation(ImmutableServiceCredentialBindingOperation.builder()
                                                                                                      .type(ServiceCredentialBindingOperation.Type.CREATE)
                                                                                                      .state(ServiceCredentialBindingOperation.State.SUCCEEDED)
                                                                                                      .build())
                                       .build();
    }

    @Override
    protected DetermineServiceKeysToRecreateStep createStep() {
        return new DetermineServiceKeysToRecreateStep();
    }
}
