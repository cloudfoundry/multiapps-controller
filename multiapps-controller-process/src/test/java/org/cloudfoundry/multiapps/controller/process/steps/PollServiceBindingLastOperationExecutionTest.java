package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sap.cloudfoundry.client.facade.domain.CloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBinding;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceBinding;
import com.sap.cloudfoundry.client.facade.domain.ImmutableServiceCredentialBindingOperation;
import com.sap.cloudfoundry.client.facade.domain.ServiceCredentialBindingOperation;

class PollServiceBindingLastOperationExecutionTest extends AsyncStepOperationTest<BindServiceToApplicationStep> {

    private static final UUID SERVICE_BINDING_APP_GUID = UUID.randomUUID();
    private static final UUID SERVICE_BINDING_SERVICE_INSTANCE_GUID = UUID.randomUUID();
    private static final UUID SERVICE_BINDING_GUID = UUID.randomUUID();
    private static final String APP_TO_PROCESS_NAME = "test-app";
    private static final UUID APP_TO_PROCESS_GUID = UUID.randomUUID();
    private static final String SERVICE_TO_UNBIND_BIND = "test-service-instance";
    private static final UUID SERVICE_TO_UNBIND_BIND_GUID = UUID.randomUUID();

    private AsyncExecutionState expectedExecutionStatus;

    static Stream<Arguments> testPollExecution() {
        return Stream.of(Arguments.of(buildCloudServiceBinding("succeeded"), AsyncExecutionState.FINISHED),
                         Arguments.of(buildCloudServiceBinding("in progress"), AsyncExecutionState.RUNNING),
                         Arguments.of(buildCloudServiceBinding("initial"), AsyncExecutionState.RUNNING),
                         Arguments.of(buildCloudServiceBinding("failed"), AsyncExecutionState.ERROR));
    }

    @ParameterizedTest
    @MethodSource
    void testPollExecution(CloudServiceBinding serviceBinding, AsyncExecutionState asyncExecutionState) {
        expectedExecutionStatus = asyncExecutionState;
        initializeParameters(serviceBinding);
        testExecuteOperations();
    }

    @Test
    void testFailedExecutionForOptionalService() {
        expectedExecutionStatus = AsyncExecutionState.FINISHED;
        context.setVariable(Variables.SERVICES_TO_BIND, List.of(buildCloudServiceInstance(SERVICE_TO_UNBIND_BIND, true)));
        initializeParameters(buildCloudServiceBinding("failed"));
        testExecuteOperations();
    }

    @Test
    void testBindingBeingDeletedButIsInProgress() {
        expectedExecutionStatus = AsyncExecutionState.RUNNING;
        CloudServiceBinding serviceBinding = buildCloudServiceBinding("in progress");
        context.setVariable(Variables.SERVICE_BINDING_TO_DELETE, serviceBinding);
        when(client.getServiceBindingForApplication(any(UUID.class), any(UUID.class))).thenReturn(serviceBinding);
        initializeParameters(serviceBinding);
        testExecuteOperations();
    }

    @Test
    void testBindingToDeleteIsAlreadyDeleted() {
        expectedExecutionStatus = AsyncExecutionState.FINISHED;
        CloudServiceBinding serviceBinding = buildCloudServiceBinding("in progress");
        context.setVariable(Variables.SERVICE_BINDING_TO_DELETE, serviceBinding);
        initializeParameters(serviceBinding);
        when(client.getServiceBindingForApplication(any(), any())).thenReturn(null);
        testExecuteOperations();
    }

    @Test
    void testPollingErrorMessage() {
        context.setVariable(Variables.APP_TO_PROCESS, buildCloudApplication());
        context.setVariable(Variables.SERVICE_TO_UNBIND_BIND, SERVICE_TO_UNBIND_BIND);
        List<AsyncExecution> asyncExecutions = getAsyncOperations(context);
        String expectedErrorMessage = MessageFormat.format(Messages.ERROR_WHILE_POLLING_SERVICE_BINDING_OPERATIONS_BETWEEN_APP_0_AND_SERVICE_INSTANCE_1,
                                                           APP_TO_PROCESS_NAME, SERVICE_TO_UNBIND_BIND);
        asyncExecutions.forEach(asyncExecution -> assertEquals(expectedErrorMessage, asyncExecution.getPollingErrorMessage(context)));
    }

    private static CloudServiceBinding buildCloudServiceBinding(String lastOperationState) {
        return ImmutableCloudServiceBinding.builder()
                                           .name("test-service-binding")
                                           .applicationGuid(SERVICE_BINDING_APP_GUID)
                                           .serviceInstanceGuid(SERVICE_BINDING_SERVICE_INSTANCE_GUID)
                                           .serviceBindingOperation(buildServiceBindingOperation(lastOperationState))
                                           .metadata(buildCloudMetadata(SERVICE_BINDING_GUID))
                                           .build();
    }

    private static ServiceCredentialBindingOperation buildServiceBindingOperation(String lastOperationState) {
        return ImmutableServiceCredentialBindingOperation.builder()
                                                         .state(ServiceCredentialBindingOperation.State.fromString(lastOperationState))
                                                         .type(ServiceCredentialBindingOperation.Type.CREATE)
                                                         .createdAt(LocalDateTime.now())
                                                         .updatedAt(LocalDateTime.now())
                                                         .description("Service binding description")
                                                         .build();
    }

    private static CloudServiceInstanceExtended buildCloudServiceInstance(String serviceInstanceName, boolean isOptional) {
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .name(serviceInstanceName)
                                                    .isOptional(isOptional)
                                                    .build();
    }

    private static CloudMetadata buildCloudMetadata(UUID guid) {
        return ImmutableCloudMetadata.builder()
                                     .createdAt(LocalDateTime.now())
                                     .updatedAt(LocalDateTime.now())
                                     .guid(guid)
                                     .build();
    }

    private void initializeParameters(CloudServiceBinding serviceBinding) {
        context.setVariable(Variables.APP_TO_PROCESS, buildCloudApplication());
        context.setVariable(Variables.SERVICE_TO_UNBIND_BIND, SERVICE_TO_UNBIND_BIND);
        when(client.getApplicationGuid(APP_TO_PROCESS_NAME)).thenReturn(APP_TO_PROCESS_GUID);
        when(client.getRequiredServiceInstanceGuid(SERVICE_TO_UNBIND_BIND)).thenReturn(SERVICE_TO_UNBIND_BIND_GUID);
        when(client.getServiceBindingForApplication(any(), any())).thenReturn(serviceBinding);
    }

    private CloudApplicationExtended buildCloudApplication() {
        return ImmutableCloudApplicationExtended.builder()
                                                .name(APP_TO_PROCESS_NAME)
                                                .build();
    }

    @Override
    protected List<AsyncExecution> getAsyncOperations(ProcessContext wrapper) {
        return List.of(new PollServiceBindingLastOperationExecution());
    }

    @Override
    protected void validateOperationExecutionResult(AsyncExecutionState result) {
        assertEquals(expectedExecutionStatus, result);
    }

    @Override
    protected BindServiceToApplicationStep createStep() {
        return new BindServiceToApplicationStep();
    }
}
