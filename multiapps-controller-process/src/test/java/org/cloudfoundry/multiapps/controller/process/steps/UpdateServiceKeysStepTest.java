package org.cloudfoundry.multiapps.controller.process.steps;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceKey;

class UpdateServiceKeysStepTest extends SyncFlowableStepTest<UpdateServiceKeysStep> {

    private static final String SERVICE_NAME = "test-service";

    static Stream<Arguments> testCreateUpdateServiceKeysStep() {
        return Stream.of(
                         // (1) There no exists service keys
                         Arguments.of(List.of("key-1", "key-2", "key-3"), Collections.emptyList(), false, Collections.emptyList()),
                         // (2) Service key "key-2" should be deleted and "key-3" updated
                         Arguments.of(List.of("key-1", "key-3"), List.of("key-1", "key-2", "key-3"), true, List.of("key-3")),
                         // (3) Service key "key-2" should be reported that cannot be deleted and "key-1" updated
                         Arguments.of(List.of("key-1", "key-3"), List.of("key-1", "key-2", "key-3"), false, List.of("key-1")));
    }

    @ParameterizedTest
    @MethodSource
    void testCreateUpdateServiceKeysStep(List<String> serviceKeysNames, List<String> existingServiceKeysNames, boolean canDeleteServiceKeys,
                                         List<String> updatedServiceKeys) {
        CloudServiceInstanceExtended service = buildService();
        List<CloudServiceKey> serviceKeys = buildServiceKeys(serviceKeysNames, updatedServiceKeys, service);
        List<CloudServiceKey> existingServiceKeys = buildServiceKeys(existingServiceKeysNames, Collections.emptyList(), service);
        prepareContext(serviceKeys, canDeleteServiceKeys, service);
        prepareClient(existingServiceKeys);

        step.execute(execution);

        verifyCreateCalls(serviceKeysNames, existingServiceKeysNames);
        verifyDeleteCalls(serviceKeysNames, existingServiceKeysNames, canDeleteServiceKeys);
        verifyUpdateCalls(updatedServiceKeys, canDeleteServiceKeys);
        assertStepFinishedSuccessfully();
    }

    private CloudServiceInstanceExtended buildService() {
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .resourceName(SERVICE_NAME)
                                                    .name(SERVICE_NAME)
                                                    .build();
    }

    private List<CloudServiceKey> buildServiceKeys(List<String> serviceKeysNames, List<String> updatedServiceKeys,
                                                   CloudServiceInstanceExtended service) {
        return serviceKeysNames.stream()
                               .map(serviceKeyName -> buildCloudServiceKey(service, updatedServiceKeys, serviceKeyName))
                               .collect(Collectors.toList());
    }

    private void prepareContext(List<CloudServiceKey> serviceKeys, boolean canDeleteServiceKeys, CloudServiceInstanceExtended service) {
        context.setVariable(Variables.SERVICE_KEYS_TO_CREATE, Map.of(SERVICE_NAME, serviceKeys));
        context.setVariable(Variables.DELETE_SERVICE_KEYS, canDeleteServiceKeys);
        context.setVariable(Variables.SERVICE_TO_PROCESS, service);

    }

    private ImmutableCloudServiceKey buildCloudServiceKey(CloudServiceInstanceExtended service, List<String> updatedServiceKeys,
                                                          String serviceKeyName) {
        if (updatedServiceKeys.contains(serviceKeyName)) {
            return ImmutableCloudServiceKey.builder()
                                           .name(serviceKeyName)
                                           .serviceInstance(service)
                                           .credentials(Map.of("name", "new-value"))
                                           .build();
        }
        return ImmutableCloudServiceKey.builder()
                                       .name(serviceKeyName)
                                       .serviceInstance(service)
                                       .build();
    }

    private void prepareClient(List<CloudServiceKey> existingServiceKeys) {
        when(client.getServiceKeysWithCredentials(anyString())).thenReturn(existingServiceKeys);
    }

    private void verifyCreateCalls(List<String> serviceKeysNames, List<String> existingServiceKeysNames) {
        verifyKeysOperations(serviceKeysNames, existingServiceKeysNames,
                             serviceKeyName -> verify(client).createServiceKey(eq(SERVICE_NAME), eq(serviceKeyName), any()));
    }

    private void verifyKeysOperations(List<String> sourceKeys, List<String> resultKeys, Consumer<String> consumer) {
        sourceKeys.stream()
                  .filter(existingServiceKeyName -> !resultKeys.contains(existingServiceKeyName))
                  .forEach(consumer);
    }

    private void verifyDeleteCalls(List<String> serviceKeysNames, List<String> existingServiceKeysNames, boolean canDeleteServiceKeys) {
        if (canDeleteServiceKeys) {
            verifyKeysOperations(existingServiceKeysNames, serviceKeysNames,
                                 existingServiceKeyName -> verify(client).deleteServiceBinding(eq(SERVICE_NAME),
                                                                                               eq(existingServiceKeyName)));
            return;
        }
        verifyKeysOperations(existingServiceKeysNames, serviceKeysNames,
                             existingServiceKeyName -> verify(stepLogger).warn(eq(Messages.WILL_NOT_DELETE_SERVICE_KEY),
                                                                               eq(existingServiceKeyName), eq(SERVICE_NAME)));
    }

    private void verifyUpdateCalls(List<String> updatedServiceKeys, boolean canDeleteServiceKeys) {
        if (canDeleteServiceKeys) {
            updatedServiceKeys.forEach(this::verifyUpdateCall);
            return;
        }
        updatedServiceKeys.forEach(this::verifyWarnCall);
    }

    private void verifyUpdateCall(String updatedServiceKeyName) {
        verify(client).deleteServiceBinding(SERVICE_NAME, updatedServiceKeyName);
        verify(client).createServiceKey(eq(SERVICE_NAME), eq(updatedServiceKeyName), any());
    }

    private void verifyWarnCall(String updatedServiceKeyName) {
        verify(stepLogger).warn(eq(Messages.WILL_NOT_UPDATE_SERVICE_KEY), eq(updatedServiceKeyName), eq(SERVICE_NAME));
    }

    @Override
    protected UpdateServiceKeysStep createStep() {
        return new UpdateServiceKeysStep();
    }

}
