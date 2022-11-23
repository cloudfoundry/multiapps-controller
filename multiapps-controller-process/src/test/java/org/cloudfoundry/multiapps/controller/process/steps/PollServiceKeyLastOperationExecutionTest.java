package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ImmutableServiceCredentialBindingOperation;
import com.sap.cloudfoundry.client.facade.domain.ServiceCredentialBindingOperation;

class PollServiceKeyLastOperationExecutionTest extends AsyncStepOperationTest<CreateServiceKeyStep> {

    private static final String SERVICE_KEY_NAME = "test_key";
    private static final UUID SERVICE_KEY_GUID = UUID.randomUUID();
    private static final String SERVICE_INSTANCE_NAME = "test_service_instance";

    private AsyncExecutionState expectedAsyncExecutionState;

    static Stream<Arguments> testPollingOfServiceKeyInDifferentStates() {
        return Stream.of(Arguments.of(ServiceCredentialBindingOperation.State.INITIAL, AsyncExecutionState.RUNNING),
                         Arguments.of(ServiceCredentialBindingOperation.State.IN_PROGRESS, AsyncExecutionState.RUNNING),
                         Arguments.of(ServiceCredentialBindingOperation.State.SUCCEEDED, AsyncExecutionState.FINISHED),
                         Arguments.of(ServiceCredentialBindingOperation.State.FAILED, AsyncExecutionState.ERROR));
    }

    @ParameterizedTest
    @MethodSource
    void testPollingOfServiceKeyInDifferentStates(ServiceCredentialBindingOperation.State serviceKeyState,
                                                  AsyncExecutionState expectedAsyncExecutionState) {
        this.expectedAsyncExecutionState = expectedAsyncExecutionState;
        CloudServiceKey serviceKey = buildCloudServiceKey(serviceKeyState, false);
        context.setVariable(Variables.SERVICE_KEY_TO_PROCESS, serviceKey);
        context.setVariable(Variables.SERVICE_TO_PROCESS, buildCloudServiceInstance(false));
        when(client.getServiceKey(SERVICE_INSTANCE_NAME, SERVICE_KEY_NAME)).thenReturn(serviceKey);
        testExecuteOperations();
    }

    @Test
    void testPollingOfFailedServiceKeyForOptionalService() {
        this.expectedAsyncExecutionState = AsyncExecutionState.FINISHED;
        CloudServiceKey serviceKey = buildCloudServiceKey(ServiceCredentialBindingOperation.State.FAILED, true);
        context.setVariable(Variables.SERVICE_KEY_TO_PROCESS, serviceKey);
        context.setVariable(Variables.SERVICE_TO_PROCESS, buildCloudServiceInstance(true));
        when(client.getServiceKey(SERVICE_INSTANCE_NAME, SERVICE_KEY_NAME)).thenReturn(serviceKey);
        testExecuteOperations();
    }

    private CloudServiceKey buildCloudServiceKey(ServiceCredentialBindingOperation.State serviceKeyState, boolean isServiceOptional) {
        return ImmutableCloudServiceKey.builder()
                                       .name(SERVICE_KEY_NAME)
                                       .metadata(ImmutableCloudMetadata.of(SERVICE_KEY_GUID))
                                       .serviceInstance(buildCloudServiceInstance(isServiceOptional))
                                       .serviceKeyOperation(ImmutableServiceCredentialBindingOperation.builder()
                                                                                                      .type(ServiceCredentialBindingOperation.Type.CREATE)
                                                                                                      .state(serviceKeyState)
                                                                                                      .build())
                                       .build();
    }

    private CloudServiceInstanceExtended buildCloudServiceInstance(boolean isOptional) {
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .name(SERVICE_INSTANCE_NAME)
                                                    .isOptional(isOptional)
                                                    .build();
    }

    @Override
    protected List<AsyncExecution> getAsyncOperations(ProcessContext wrapper) {
        return List.of(new PollServiceKeyLastOperationExecution());
    }

    @Override
    protected void validateOperationExecutionResult(AsyncExecutionState result) {
        assertEquals(expectedAsyncExecutionState, result);
    }

    @Override
    protected CreateServiceKeyStep createStep() {
        return new CreateServiceKeyStep();
    }
}
