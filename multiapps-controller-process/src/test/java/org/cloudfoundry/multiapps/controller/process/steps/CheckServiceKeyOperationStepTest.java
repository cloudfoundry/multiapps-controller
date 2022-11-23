package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ImmutableServiceCredentialBindingOperation;
import com.sap.cloudfoundry.client.facade.domain.ServiceCredentialBindingOperation;

class CheckServiceKeyOperationStepTest extends SyncFlowableStepTest<CheckServiceKeyOperationStep> {

    private static final String SERVICE_KEY_NAME = "test-service-key";
    private static final String SERVICE_INSTANCE_NAME = "test-service-instance";

    @Test
    void testCheckServiceKeyThatDoesNotExist() {
        CloudServiceKey serviceKey = buildCloudServiceKey(ServiceCredentialBindingOperation.State.SUCCEEDED);
        context.setVariable(Variables.SERVICE_KEY_TO_PROCESS, serviceKey);

        step.execute(execution);
        assertStepFinishedSuccessfully();
    }

    static Stream<Arguments> testCheckServiceKeyInDifferentStates() {
        return Stream.of(Arguments.of(ServiceCredentialBindingOperation.State.INITIAL, StepPhase.POLL),
                         Arguments.of(ServiceCredentialBindingOperation.State.IN_PROGRESS, StepPhase.POLL),
                         Arguments.of(ServiceCredentialBindingOperation.State.FAILED, StepPhase.DONE),
                         Arguments.of(ServiceCredentialBindingOperation.State.SUCCEEDED, StepPhase.DONE));
    }

    @MethodSource
    @ParameterizedTest
    void testCheckServiceKeyInDifferentStates(ServiceCredentialBindingOperation.State serviceKeyState, StepPhase expectedStepPhase) {
        CloudServiceKey serviceKey = buildCloudServiceKey(serviceKeyState);
        context.setVariable(Variables.SERVICE_KEY_TO_PROCESS, serviceKey);
        when(client.getServiceKey(SERVICE_INSTANCE_NAME, SERVICE_KEY_NAME)).thenReturn(serviceKey);
        step.execute(execution);
        assertEquals(expectedStepPhase.toString(), getExecutionStatus());
    }

    @Test
    void testGetStepErrorMessage() {
        CloudServiceKey serviceKey = buildCloudServiceKey(ServiceCredentialBindingOperation.State.FAILED);
        context.setVariable(Variables.SERVICE_KEY_TO_PROCESS, serviceKey);
        assertEquals("Error while checking service key operation \"test-service-key\"", step.getStepErrorMessage(context));
    }

    private CloudServiceKey buildCloudServiceKey(ServiceCredentialBindingOperation.State serviceKeyState) {
        return ImmutableCloudServiceKey.builder()
                                       .name(SERVICE_KEY_NAME)
                                       .serviceInstance(buildCloudServiceInstanceExtended())
                                       .serviceKeyOperation(ImmutableServiceCredentialBindingOperation.builder()
                                                                                                      .state(serviceKeyState)
                                                                                                      .type(ServiceCredentialBindingOperation.Type.CREATE)
                                                                                                      .build())
                                       .build();
    }

    private CloudServiceInstanceExtended buildCloudServiceInstanceExtended() {
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .name(SERVICE_INSTANCE_NAME)
                                                    .build();
    }

    @Override
    protected CheckServiceKeyOperationStep createStep() {
        return new CheckServiceKeyOperationStep();
    }
}
