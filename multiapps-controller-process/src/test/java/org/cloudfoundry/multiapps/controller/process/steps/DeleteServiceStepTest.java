package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.util.ServiceAction;
import org.cloudfoundry.multiapps.controller.process.util.ServiceOperationGetter;
import org.cloudfoundry.multiapps.controller.process.util.ServiceProgressReporter;
import org.cloudfoundry.multiapps.controller.process.util.ServiceRemover;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ServiceOperation;

class DeleteServiceStepTest extends SyncFlowableStepTest<DeleteServiceStep> {

    private static final String SERVICE_NAME = "test-service";
    private static final String SERVICE_GUID = "5ee63aa7-fb56-4e8f-b43f-a74efead2602";

    private ServiceProgressReporter serviceProgressReporter;
    private ServiceRemover serviceRemover;
    private ServiceOperationGetter serviceOperationGetter;

    // @formatter:off
    static Stream<Arguments> testServiceDelete() {
        return Stream.of(Arguments.of(false, StepPhase.POLL),
                         Arguments.of(true, StepPhase.POLL));
    }
    // @formatter:on

    @ParameterizedTest
    @MethodSource
    void testServiceDelete(boolean shouldRecreateService, StepPhase expectedStepPhase) {
        prepareActionsToExecute(shouldRecreateService);
        UUID serviceGuid = UUID.fromString(SERVICE_GUID);
        prepareContext();
        CloudServiceInstance cloudServiceInstance = createCloudService(serviceGuid);
        prepareClient(cloudServiceInstance);
        step.execute(context.getExecution());
        assertStepPhase(expectedStepPhase);
        assertServiceRemoverCall(expectedStepPhase);
    }

    private void prepareActionsToExecute(boolean shouldRecreateService) {
        List<ServiceAction> actionsToExecute = new ArrayList<>();
        if (shouldRecreateService) {
            actionsToExecute.add(ServiceAction.RECREATE);
        }
        context.setVariable(Variables.SERVICE_ACTIONS_TO_EXCECUTE, actionsToExecute);
    }

    @Test
    void testServicePolling() {
        UUID serviceGuid = UUID.fromString(SERVICE_GUID);
        prepareContext();
        CloudServiceInstance serviceInstance = createCloudService(serviceGuid);
        prepareClient(serviceInstance);
        ServiceOperation lastOp = new ServiceOperation(ServiceOperation.Type.DELETE, "", ServiceOperation.State.SUCCEEDED);
        when(serviceOperationGetter.getLastServiceOperation(any(), any())).thenReturn(lastOp);

        step.execute(context.getExecution());
        assertStepPhase(StepPhase.POLL);

        step.execute(context.getExecution());
        assertStepPhase(StepPhase.DONE);
    }

    private void prepareContext() {
        context.setVariable(Variables.SERVICE_TO_DELETE, SERVICE_NAME);
        context.setVariable(Variables.DELETE_SERVICES, true);
    }

    private CloudServiceInstance createCloudService(UUID serviceGuid) {
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .metadata(ImmutableCloudMetadata.of(serviceGuid))
                                                    .name(SERVICE_NAME)
                                                    .build();
    }

    private void prepareClient(CloudServiceInstance serviceInstance) {
        when(client.getServiceInstanceWithoutAuxiliaryContent(eq(SERVICE_NAME))).thenReturn(serviceInstance);
    }

    private void assertStepPhase(StepPhase expectedStepPhase) {
        assertEquals(expectedStepPhase.toString(), getExecutionStatus());
    }

    private void assertServiceRemoverCall(StepPhase expectedStepPhase) {
        int callTimes = expectedStepPhase.equals(StepPhase.DONE) ? 0 : 1;
        verify(serviceRemover, times(callTimes)).deleteService(any(), any());
    }

    @Override
    protected DeleteServiceStep createStep() {
        serviceOperationGetter = mock(ServiceOperationGetter.class);
        serviceProgressReporter = mock(ServiceProgressReporter.class);
        serviceRemover = mock(ServiceRemover.class);
        return new DeleteServiceStep(serviceOperationGetter, serviceProgressReporter, serviceRemover);
    }

}
