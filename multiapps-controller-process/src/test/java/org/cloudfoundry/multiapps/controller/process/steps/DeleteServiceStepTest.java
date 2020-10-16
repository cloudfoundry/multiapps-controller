package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ServiceAction;
import org.cloudfoundry.multiapps.controller.process.util.ServiceOperationGetter;
import org.cloudfoundry.multiapps.controller.process.util.ServiceProgressReporter;
import org.cloudfoundry.multiapps.controller.process.util.ServiceRemover;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

import com.sap.cloudfoundry.client.facade.domain.CloudServiceBinding;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceBinding;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ServiceOperation;

class DeleteServiceStepTest extends SyncFlowableStepTest<DeleteServiceStep> {

    private static final String SEVICE_KEY_NAME = "test-service-key";
    private static final String SERVICE_NAME = "test-service";
    private static final String SERVICE_GUID = "5ee63aa7-fb56-4e8f-b43f-a74efead2602";

    @Mock
    private ServiceProgressReporter serviceProgressReporter;
    @Mock
    private ServiceRemover serviceRemover;
    @Mock
    private ServiceOperationGetter serviceOperationGetter;

    static Stream<Arguments> testServiceDelete() {
        return Stream.of(Arguments.of(true, false, true, true, StepPhase.DONE), Arguments.of(false, false, true, true, StepPhase.DONE),
                         Arguments.of(false, false, false, true, StepPhase.DONE), Arguments.of(false, false, false, false, StepPhase.POLL),
                         Arguments.of(true, false, false, true, StepPhase.DONE), Arguments.of(true, false, true, false, StepPhase.POLL),
                         Arguments.of(false, true, true, true, StepPhase.DONE), Arguments.of(false, true, false, true, StepPhase.POLL),
                         Arguments.of(false, true, false, false, StepPhase.POLL));
    }

    @ParameterizedTest
    @MethodSource
    void testServiceDelete(boolean shouldRecreateService, boolean shouldDeleteServiceKeys, boolean hasServiceBindings,
                           boolean hasServiceKeys, StepPhase expectedStepPhase) {
        prepareActionsToExecute(shouldRecreateService, shouldDeleteServiceKeys);
        UUID serviceGuid = UUID.fromString(SERVICE_GUID);
        prepareContext();
        CloudServiceInstance cloudServiceInstance = createCloudService(serviceGuid);
        List<CloudServiceKey> serviceKeys = createServiceKeys(cloudServiceInstance, hasServiceKeys);
        prepareClient(cloudServiceInstance, serviceKeys, hasServiceBindings);

        step.execute(context.getExecution());

        assertStepPhase(expectedStepPhase);
        assertServiceRemoverCall(expectedStepPhase);
    }

    private void prepareActionsToExecute(boolean shouldRecreateService, boolean shouldDeleteServiceKeys) {
        List<ServiceAction> actionsToExecute = new ArrayList<>();
        if (shouldRecreateService) {
            actionsToExecute.add(ServiceAction.RECREATE);
        }
        context.setVariable(Variables.SERVICE_ACTIONS_TO_EXCECUTE, actionsToExecute);
        context.setVariable(Variables.DELETE_SERVICE_KEYS, shouldDeleteServiceKeys);
    }

    @Test
    void testWithNullVariable() {
        step.execute(context.getExecution());

        verify(stepLogger).debug(Messages.MISSING_SERVICE_TO_DELETE);
        assertStepFinishedSuccessfully();
    }

    @Test
    void testServicePolling() {
        UUID serviceGuid = UUID.fromString(SERVICE_GUID);
        prepareContext();
        CloudServiceInstance serviceInstance = createCloudService(serviceGuid);
        prepareClient(serviceInstance, Collections.emptyList(), false);
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
                                                    .metadata(ImmutableCloudMetadata.builder()
                                                                                    .guid(serviceGuid)
                                                                                    .build())
                                                    .name(SERVICE_NAME)
                                                    .build();
    }

    private void prepareClient(CloudServiceInstance serviceInstance, List<CloudServiceKey> serviceKeys, boolean hasServiceBindings) {
        when(client.getServiceInstance(eq(SERVICE_NAME), anyBoolean())).thenReturn(serviceInstance);
        when(client.getServiceKeys(serviceInstance)).thenReturn(serviceKeys);
        if (hasServiceBindings) {
            when(client.getServiceBindings(serviceInstance.getMetadata()
                                                          .getGuid())).thenReturn(List.of(createServiceBinding()));
        }
    }

    private CloudServiceBinding createServiceBinding() {
        return ImmutableCloudServiceBinding.builder()
                                           .applicationGuid(UUID.randomUUID())
                                           .build();
    }

    private List<CloudServiceKey> createServiceKeys(CloudServiceInstance serviceInstance, boolean hasServiceKeys) {
        if (hasServiceKeys) {
            return List.of(ImmutableCloudServiceKey.builder()
                                                   .name(SEVICE_KEY_NAME)
                                                   .serviceInstance(serviceInstance)
                                                   .build());
        }
        return Collections.emptyList();
    }

    private void assertStepPhase(StepPhase expectedStepPhase) {
        assertEquals(expectedStepPhase.toString(), getExecutionStatus());
    }

    private void assertServiceRemoverCall(StepPhase expectedStepPhase) {
        int callTimes = expectedStepPhase.equals(StepPhase.DONE) ? 0 : 1;
        verify(serviceRemover, times(callTimes)).deleteService(any(), any(), anyList(), anyList());
    }

    @Override
    protected DeleteServiceStep createStep() {
        return new DeleteServiceStep(serviceOperationGetter, serviceProgressReporter, serviceRemover);
    }

}
