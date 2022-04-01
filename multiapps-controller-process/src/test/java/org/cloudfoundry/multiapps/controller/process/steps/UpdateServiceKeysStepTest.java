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

import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataAnnotations;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceKey;

class UpdateServiceKeysStepTest extends SyncFlowableStepTest<UpdateServiceKeysStep> {

    private static final String SERVICE_NAME = "test-service";

    // to use a namespace for the test, add [namespace]key-name in the key name
    static Stream<Arguments> testCreateUpdateServiceKeysStep() {
        return Stream.of(
                         // (1) There no exists service keys
                         Arguments.of(List.of("key-1", "key-2", "key-3"), Collections.emptyList(), false, Collections.emptyList(),
                                      Collections.emptyList()),
                         // (2) Service key "key-2" should be deleted and "key-3" updated
                         Arguments.of(List.of("key-1", "key-3"), List.of("key-1", "key-2", "key-3"), true, List.of("key-3"),
                                      List.of("key-2")),
                         // (3) Service key "key-2" should be reported that cannot be deleted and "key-1" updated
                         Arguments.of(List.of("key-1", "key-3"), List.of("key-1", "key-2", "key-3"), false, List.of("key-1"),
                                      List.of("key-2")),
                         // (4) Service key "key-2" should be be deleted but "key-3" shouldn't since it has a different namespace
                         Arguments.of(List.of("[new-namespace]key-1"), List.of("[new-namespace]key-2", "[old-namespace]key-3"), true,
                                      Collections.emptyList(), List.of("[new-namespace]key-2")));
    }

    @ParameterizedTest
    @MethodSource
    void testCreateUpdateServiceKeysStep(List<String> newServiceKeyNames, List<String> existingServiceKeyNames,
                                         boolean canDeleteServiceKeys, List<String> updatedServiceKeys, List<String> deletedServiceKeys) {
        CloudServiceInstanceExtended service = buildService();
        List<CloudServiceKey> serviceKeysToCreate = buildServiceKeys(newServiceKeyNames, updatedServiceKeys, service);
        List<CloudServiceKey> existingServiceKeys = buildServiceKeys(existingServiceKeyNames, Collections.emptyList(), service);
        prepareContext(serviceKeysToCreate, canDeleteServiceKeys, service, getKeyNamespace(newServiceKeyNames.get(0)));
        prepareClient(existingServiceKeys);

        step.execute(execution);

        verifyCreateCalls(newServiceKeyNames, existingServiceKeyNames);
        verifyDeleteCalls(deletedServiceKeys, canDeleteServiceKeys);
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
                               .map(serviceKeyName -> buildCloudServiceKey(service, updatedServiceKeys, serviceKeyName,
                                                                           getKeyNamespace(serviceKeyName)))
                               .collect(Collectors.toList());
    }

    private static String getKeyNamespace(String keyName) {
        if (keyName.startsWith("[")) {
            return keyName.substring(1, keyName.indexOf("]"));
        }

        return "";
    }

    private static String getKeyName(String keyName) {
        if (keyName.startsWith("[")) {
            return keyName.substring(keyName.indexOf("]") + 1);
        }

        return keyName;
    }

    private void prepareContext(List<CloudServiceKey> serviceKeysToCreate, boolean canDeleteServiceKeys,
                                CloudServiceInstanceExtended service, String namespace) {
        context.setVariable(Variables.SERVICE_KEYS_TO_CREATE, Map.of(SERVICE_NAME, serviceKeysToCreate));
        context.setVariable(Variables.DELETE_SERVICE_KEYS, canDeleteServiceKeys);
        context.setVariable(Variables.SERVICE_TO_PROCESS, service);
        context.setVariable(Variables.MTA_NAMESPACE, namespace);

    }

    private ImmutableCloudServiceKey buildCloudServiceKey(CloudServiceInstanceExtended service, List<String> updatedServiceKeys,
                                                          String serviceKeyName, String namespace) {
        Metadata metadata = Metadata.builder()
                                    .annotation(MtaMetadataAnnotations.MTA_NAMESPACE, namespace)
                                    .build();

        if (updatedServiceKeys.contains(serviceKeyName)) {
            return ImmutableCloudServiceKey.builder()
                                           .name(getKeyName(serviceKeyName))
                                           .serviceInstance(service)
                                           .credentials(Map.of("name", "new-value"))
                                           .v3Metadata(metadata)
                                           .build();
        }
        return ImmutableCloudServiceKey.builder()
                                       .name(getKeyName(serviceKeyName))
                                       .serviceInstance(service)
                                       .v3Metadata(metadata)
                                       .build();
    }

    private void prepareClient(List<CloudServiceKey> existingServiceKeys) {
        when(client.getServiceKeysWithCredentials(anyString())).thenReturn(existingServiceKeys);
    }

    private void verifyCreateCalls(List<String> serviceKeysNames, List<String> existingServiceKeysNames) {
        verifyKeysOperations(serviceKeysNames, existingServiceKeysNames,
                             serviceKeyName -> verify(client).createServiceKey(eq(SERVICE_NAME), eq(getKeyName(serviceKeyName)), any()));
    }

    private void verifyKeysOperations(List<String> sourceKeys, List<String> excludedKeys, Consumer<String> consumer) {
        sourceKeys.stream()
                  .filter(sourceKeyName -> !excludedKeys.contains(sourceKeyName))
                  .forEach(consumer);
    }

    private void verifyDeleteCalls(List<String> deletedServiceKeysNames, boolean canDeleteServiceKeys) {
        if (canDeleteServiceKeys) {
            verifyKeysOperations(deletedServiceKeysNames, Collections.emptyList(),
                                 serviceKeyName -> verify(client).deleteServiceBinding(eq(SERVICE_NAME),
                                                                                               eq(getKeyName(serviceKeyName))));
            return;
        }
        verifyKeysOperations(deletedServiceKeysNames, Collections.emptyList(),
                             serviceKeyName -> verify(stepLogger).warn(eq(Messages.WILL_NOT_DELETE_SERVICE_KEY),
                                                                               eq(getKeyName(serviceKeyName)), eq(SERVICE_NAME)));
    }

    private void verifyUpdateCalls(List<String> updatedServiceKeys, boolean canDeleteServiceKeys) {
        if (canDeleteServiceKeys) {
            updatedServiceKeys.forEach(keyName -> verifyUpdateCall(getKeyName(keyName)));
            return;
        }
        updatedServiceKeys.forEach(keyName -> verifyWarnCall(getKeyName(keyName)));
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
