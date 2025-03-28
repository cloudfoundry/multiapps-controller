package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBinding;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceBinding;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceInstance;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ImmutableServiceCredentialBindingOperation;
import com.sap.cloudfoundry.client.facade.domain.ServiceCredentialBindingOperation;

class PollServiceBindingOrKeyOperationExecutionTest extends AsyncStepOperationTest<CheckForServiceBindingOrKeyOperationStep> {

    private final static String SERVICE_NAME = "serviceName";
    private final static UUID SERVICE_UUID = UUID.randomUUID();
    private AsyncExecutionState expectedExecutionStatus;
    private CloudControllerClient controllerClient;
    private CloudServiceInstance serviceInstance;
    private List<CloudServiceBinding> serviceBindings;
    private List<CloudServiceKey> serviceKeys;

    @BeforeEach
    void initialize() {
        serviceInstance = buildServiceInstance();
        context.setVariable(Variables.SERVICE_WITH_BIND_IN_PROGRESS, SERVICE_NAME);

        controllerClient = Mockito.mock(CloudControllerClient.class);

        Mockito.when(context.getControllerClient())
               .thenReturn(controllerClient);
        Mockito.when(controllerClient.getServiceInstance(SERVICE_NAME))
               .thenReturn(serviceInstance);
    }

    @Test
    void testWithSucceededBindingsAndKeys() {
        expectedExecutionStatus = AsyncExecutionState.FINISHED;
        setUpServiceBindings(ServiceCredentialBindingOperation.State.SUCCEEDED);
        setUpServiceKeys(ServiceCredentialBindingOperation.State.SUCCEEDED);

        context.setVariable(Variables.IS_SERVICE_BINDING_KEY_OPERATION_IN_PROGRESS, false);
        testExecuteOperations();
    }

    @Test
    void testWithSucceededBindingsAndInProgressKeys() {
        expectedExecutionStatus = AsyncExecutionState.RUNNING;
        setUpServiceBindings(ServiceCredentialBindingOperation.State.SUCCEEDED);
        setUpServiceKeys(ServiceCredentialBindingOperation.State.IN_PROGRESS);

        context.setVariable(Variables.IS_SERVICE_BINDING_KEY_OPERATION_IN_PROGRESS, false);
        testExecuteOperations();
    }

    @Test
    void testWithInProgressBindingsAndSucceededKeys() {
        expectedExecutionStatus = AsyncExecutionState.RUNNING;
        setUpServiceBindings(ServiceCredentialBindingOperation.State.IN_PROGRESS);
        setUpServiceKeys(ServiceCredentialBindingOperation.State.SUCCEEDED);

        context.setVariable(Variables.IS_SERVICE_BINDING_KEY_OPERATION_IN_PROGRESS, false);
        testExecuteOperations();
    }

    @Test
    void testWithInProgressBindingsAndKeys() {
        expectedExecutionStatus = AsyncExecutionState.RUNNING;
        setUpServiceBindings(ServiceCredentialBindingOperation.State.IN_PROGRESS);
        setUpServiceKeys(ServiceCredentialBindingOperation.State.IN_PROGRESS);

        context.setVariable(Variables.IS_SERVICE_BINDING_KEY_OPERATION_IN_PROGRESS, false);
        testExecuteOperations();
    }

    @Override
    protected List<AsyncExecution> getAsyncOperations(ProcessContext wrapper) {
        return List.of(new PollServiceBindingOrKeyOperationExecution());
    }

    @Override
    protected void validateOperationExecutionResult(AsyncExecutionState result) {
        assertEquals(expectedExecutionStatus, result);
    }

    @Override
    protected CheckForServiceBindingOrKeyOperationStep createStep() {
        return new CheckForServiceBindingOrKeyOperationStep();
    }

    private void setUpServiceBindings(ServiceCredentialBindingOperation.State state) {
        serviceBindings = List.of(buildServiceBinding(state));
        Mockito.when(controllerClient.getServiceAppBindings(serviceInstance.getGuid()))
               .thenReturn(serviceBindings);
    }

    private void setUpServiceKeys(ServiceCredentialBindingOperation.State state) {
        serviceKeys = List.of(buildServiceKey(state));
        Mockito.when(controllerClient.getServiceKeys(SERVICE_NAME))
               .thenReturn(serviceKeys);
    }

    private CloudServiceInstance buildServiceInstance() {
        return ImmutableCloudServiceInstance.builder()
                                            .name(SERVICE_NAME)
                                            .metadata(ImmutableCloudMetadata.builder()
                                                                            .guid(SERVICE_UUID)
                                                                            .build())
                                            .build();
    }

    private CloudServiceBinding buildServiceBinding(ServiceCredentialBindingOperation.State state) {
        return ImmutableCloudServiceBinding.builder()
                                           .serviceInstanceGuid(serviceInstance.getGuid())
                                           .serviceBindingOperation(ImmutableServiceCredentialBindingOperation.builder()
                                                                                                              .state(state)
                                                                                                              .type(
                                                                                                                  ServiceCredentialBindingOperation.Type.CREATE)
                                                                                                              .build())
                                           .build();
    }

    private CloudServiceKey buildServiceKey(ServiceCredentialBindingOperation.State state) {
        return ImmutableCloudServiceKey.builder()
                                       .serviceInstance(serviceInstance)
                                       .serviceKeyOperation(ImmutableServiceCredentialBindingOperation.builder()
                                                                                                      .state(state)
                                                                                                      .type(
                                                                                                          ServiceCredentialBindingOperation.Type.CREATE)
                                                                                                      .build())
                                       .build();
    }
}
