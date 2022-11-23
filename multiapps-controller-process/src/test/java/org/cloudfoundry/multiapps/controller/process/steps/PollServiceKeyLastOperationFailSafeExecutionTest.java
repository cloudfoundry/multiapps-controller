package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;

import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ImmutableServiceCredentialBindingOperation;
import com.sap.cloudfoundry.client.facade.domain.ServiceCredentialBindingOperation;

class PollServiceKeyLastOperationFailSafeExecutionTest extends AsyncStepOperationTest<CheckServiceKeyOperationStep> {

    private static final String SERVICE_KEY_NAME = "test_key";
    private static final UUID SERVICE_KEY_GUID = UUID.randomUUID();
    private static final String SERVICE_INSTANCE_NAME = "test_service_instance";

    private AsyncExecutionState expectedAsyncExecutionState;

    @Test
    void testPollingOfFailedServiceKey() {
        this.expectedAsyncExecutionState = AsyncExecutionState.FINISHED;
        CloudServiceKey serviceKey = buildCloudServiceKey(ServiceCredentialBindingOperation.State.FAILED);
        context.setVariable(Variables.SERVICE_KEY_TO_PROCESS, serviceKey);
        context.setVariable(Variables.SERVICE_TO_PROCESS, buildCloudServiceInstance());
        when(client.getServiceKey(SERVICE_INSTANCE_NAME, SERVICE_KEY_NAME)).thenReturn(serviceKey);
        testExecuteOperations();
    }

    private CloudServiceKey buildCloudServiceKey(ServiceCredentialBindingOperation.State serviceKeyState) {
        return ImmutableCloudServiceKey.builder()
                                       .name(SERVICE_KEY_NAME)
                                       .metadata(ImmutableCloudMetadata.of(SERVICE_KEY_GUID))
                                       .serviceInstance(buildCloudServiceInstance())
                                       .serviceKeyOperation(ImmutableServiceCredentialBindingOperation.builder()
                                                                                                      .type(ServiceCredentialBindingOperation.Type.CREATE)
                                                                                                      .state(serviceKeyState)
                                                                                                      .build())
                                       .build();
    }

    private CloudServiceInstanceExtended buildCloudServiceInstance() {
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .name(SERVICE_INSTANCE_NAME)
                                                    .build();
    }

    @Override
    protected List<AsyncExecution> getAsyncOperations(ProcessContext wrapper) {
        return List.of(new PollServiceKeyLastOperationFailSafeExecution());
    }

    @Override
    protected void validateOperationExecutionResult(AsyncExecutionState result) {
        assertEquals(expectedAsyncExecutionState, result);
    }

    @Override
    protected CheckServiceKeyOperationStep createStep() {
        return new CheckServiceKeyOperationStep();
    }
}
