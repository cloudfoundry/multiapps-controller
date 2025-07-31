package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableServiceCredentialBindingOperation;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceCredentialBindingOperation;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class CalculateServiceKeyForWaitingStepTest extends SyncFlowableStepTest<CalculateServiceKeyForWaitingStep> {

    private static final String SERVICE_INSTANCE_NAME = "test-service";

    @Test
    void testExecuteStep() {
        context.setVariable(Variables.SERVICE_TO_PROCESS, buildCloudServiceInstanceExtended());
        CloudServiceKey serviceKeyInProgress = buildCloudServiceKey("key-in-progress", ServiceCredentialBindingOperation.State.IN_PROGRESS);
        CloudServiceKey serviceKeyInSucceededState = buildCloudServiceKey("key-succeeded",
                                                                          ServiceCredentialBindingOperation.State.SUCCEEDED);
        when(client.getServiceKeysWithCredentials(SERVICE_INSTANCE_NAME)).thenReturn(
            List.of(serviceKeyInProgress, serviceKeyInSucceededState));
        step.execute(execution);
        assertStepFinishedSuccessfully();
        assertEquals(List.of(serviceKeyInProgress), context.getVariable(Variables.CLOUD_SERVICE_KEYS_FOR_WAITING));
    }

    private CloudServiceInstanceExtended buildCloudServiceInstanceExtended() {
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .name(SERVICE_INSTANCE_NAME)
                                                    .build();
    }

    private CloudServiceKey buildCloudServiceKey(String name, ServiceCredentialBindingOperation.State state) {
        return ImmutableCloudServiceKey.builder()
                                       .name(name)
                                       .serviceKeyOperation(ImmutableServiceCredentialBindingOperation.builder()
                                                                                                      .state(state)
                                                                                                      .type(
                                                                                                          ServiceCredentialBindingOperation.Type.CREATE)
                                                                                                      .build())
                                       .build();
    }

    @Override
    protected CalculateServiceKeyForWaitingStep createStep() {
        return new CalculateServiceKeyForWaitingStep();
    }
}
