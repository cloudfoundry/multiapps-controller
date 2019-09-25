package com.sap.cloud.lm.sl.cf.process.steps;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.client.lib.domain.CloudServiceKey;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceKey;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudServiceExtended;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ServiceOperationExecutor;
import com.sap.cloud.lm.sl.common.util.MapUtil;

public class UpdateServiceKeysStepTest extends SyncFlowableStepTest<UpdateServiceKeysStep> {

    private static final String SERVICE_NAME = "test-service";

    @Mock
    private ServiceOperationExecutor serviceOperationExecutor;

    public static Stream<Arguments> testCreateUpdateServiceKeysStep() {
        return Stream.of(
        // @formatter:off
                         // (1) There no exists service keys
                         Arguments.of(Arrays.asList("key-1", "key-2", "key-3"), Collections.emptyList(), false, Collections.emptyList()),
                         // (2) Service key "key-2" should be deleted and "key-3" updated
                         Arguments.of(Arrays.asList("key-1", "key-3"), Arrays.asList("key-1", "key-2", "key-3"), true, Arrays.asList("key-3")),
                         // (3) Service key "key-2" should be reported that cannot be deleted and "key-1" updated
                         Arguments.of(Arrays.asList("key-1", "key-3"), Arrays.asList("key-1", "key-2", "key-3"), false, Arrays.asList("key-1"))
                         
        // @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testCreateUpdateServiceKeysStep(List<String> serviceKeysNames, List<String> existingServiceKeysNames,
                                                boolean canDeleteServiceKeys, List<String> updatedServiceKeys) {
        CloudServiceExtended service = buildService();
        List<CloudServiceKey> serviceKeys = buildServiceKeys(serviceKeysNames, updatedServiceKeys, service);
        List<CloudServiceKey> existingServiceKeys = buildServiceKeys(existingServiceKeysNames, Collections.emptyList(), service);
        prepareContext(serviceKeys, canDeleteServiceKeys, service);
        prepareServiceOperationExecutor(existingServiceKeys);

        step.execute(context);

        verifyCreateCalls(serviceKeysNames, existingServiceKeysNames);
        verifyDeleteCalls(serviceKeysNames, existingServiceKeysNames, canDeleteServiceKeys);
        verifyUpdateCalls(updatedServiceKeys, canDeleteServiceKeys);
        assertStepFinishedSuccessfully();
    }

    private CloudServiceExtended buildService() {
        return ImmutableCloudServiceExtended.builder()
                                            .resourceName(SERVICE_NAME)
                                            .name(SERVICE_NAME)
                                            .build();
    }

    private List<CloudServiceKey> buildServiceKeys(List<String> serviceKeysNames, List<String> updatedServiceKeys,
                                                   CloudServiceExtended service) {
        return serviceKeysNames.stream()
                               .map(serviceKeyName -> buildCloudServiceKey(service, updatedServiceKeys, serviceKeyName))
                               .collect(Collectors.toList());
    }

    private void prepareContext(List<CloudServiceKey> serviceKeys, boolean canDeleteServiceKeys, CloudServiceExtended service) {
        StepsUtil.setServiceKeysToCreate(context, MapUtil.asMap(SERVICE_NAME, serviceKeys));
        context.setVariable(Constants.PARAM_DELETE_SERVICE_KEYS, canDeleteServiceKeys);
        StepsUtil.setServiceToProcess(service, context);

    }

    private ImmutableCloudServiceKey buildCloudServiceKey(CloudServiceExtended service, List<String> updatedServiceKeys,
                                                          String serviceKeyName) {
        if (updatedServiceKeys.contains(serviceKeyName)) {
            return ImmutableCloudServiceKey.builder()
                                           .name(serviceKeyName)
                                           .service(service)
                                           .parameters(MapUtil.asMap("name", "new-value"))
                                           .build();
        }
        return ImmutableCloudServiceKey.builder()
                                       .name(serviceKeyName)
                                       .service(service)
                                       .build();
    }

    private void prepareServiceOperationExecutor(List<CloudServiceKey> existingServiceKeys) {
        when(serviceOperationExecutor.executeServiceOperation(any(), ArgumentMatchers.<Supplier<List<CloudServiceKey>>> any(),
                                                              any())).thenReturn(existingServiceKeys);
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
                                 existingServiceKeyName -> verify(client).deleteServiceKey(eq(SERVICE_NAME), eq(existingServiceKeyName)));
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
        verify(client).deleteServiceKey(eq(SERVICE_NAME), eq(updatedServiceKeyName));
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
