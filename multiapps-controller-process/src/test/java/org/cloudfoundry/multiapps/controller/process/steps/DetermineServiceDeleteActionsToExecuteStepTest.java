package org.cloudfoundry.multiapps.controller.process.steps;

import static org.cloudfoundry.multiapps.controller.process.util.ServiceDeletionActions.DELETE_METADATA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.cf.v2.ResourceType;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.process.util.ProcessTypeParser;
import org.cloudfoundry.multiapps.controller.process.util.ServiceDeletionActions;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.junit.jupiter.api.Test;

import com.sap.cloudfoundry.client.facade.domain.CloudServiceBinding;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceBinding;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ImmutableServiceCredentialBindingOperation;
import com.sap.cloudfoundry.client.facade.domain.ServiceCredentialBindingOperation;

class DetermineServiceDeleteActionsToExecuteStepTest extends SyncFlowableStepTest<DetermineServiceDeleteActionsToExecuteStep> {

    private static final UUID SERVICE_INSTANCE_GUID = UUID.randomUUID();
    private static final UUID APPLICATION_GUID = UUID.randomUUID();
    private static final String SERVICE_TO_DELETE = "serviceToDelete";

    private ProcessTypeParser processTypeParser;

    @Test
    void testWhenServiceInstanceToDeleteIsNotSet() {
        step.execute(execution);
        assertEquals(Collections.emptyList(), context.getVariable(Variables.SERVICE_DELETION_ACTIONS));
        assertStepFinishedSuccessfully();
    }

    @Test
    void testWhenServiceInstanceIsAlreadyDeleted() {
        context.setVariable(Variables.SERVICE_TO_DELETE, SERVICE_TO_DELETE);
        step.execute(execution);
        assertStepFinishedSuccessfully();
    }

    @Test
    void testExistingServiceInstanceDeletion() {
        prepareServiceInstance(buildOptionalCloudServiceInstance());
        mockDeploymentDescriptorResources();
        step.execute(execution);
        assertStepFinishedSuccessfully();
        assertEquals(List.of(DELETE_METADATA), context.getVariable(Variables.SERVICE_DELETION_ACTIONS));
    }

    private void prepareServiceInstance(CloudServiceInstanceExtended serviceInstance) {
        context.setVariable(Variables.SERVICE_TO_DELETE, SERVICE_TO_DELETE);
        when(client.getServiceInstance(SERVICE_TO_DELETE, false)).thenReturn(serviceInstance);
    }

    @Test
    void testDeleteServiceInstanceWhichHasBindings() {
        prepareServiceInstance(buildCloudServiceInstance());
        when(client.getServiceAppBindings(any())).thenReturn(List.of(buildCloudServiceBinding()));
        step.execute(execution);
        assertStepFinishedSuccessfully();
        assertEquals(List.of(DELETE_METADATA), context.getVariable(Variables.SERVICE_DELETION_ACTIONS));
    }

    @Test
    void testDeleteServiceInstanceWhichHasServiceKeys() {
        prepareServiceInstance(buildCloudServiceInstance());
        when(client.getServiceKeys(any(CloudServiceInstanceExtended.class))).thenReturn(List.of(buildCloudServiceKey()));
        step.execute(execution);
        assertStepFinishedSuccessfully();
        assertEquals(Collections.emptyList(), context.getVariable(Variables.SERVICE_DELETION_ACTIONS));
    }

    @Test
    void testDeleteServiceInstanceWithNoBindingsAndKeys() {
        prepareServiceInstance(buildCloudServiceInstance());
        step.execute(execution);
        assertStepFinishedSuccessfully();
        assertEquals(List.of(ServiceDeletionActions.DELETE_SERVICE_BINDINGS, ServiceDeletionActions.DELETE_SERVICE_KEYS,
                             ServiceDeletionActions.DELETE_SERVICE_INSTANCE),
                     context.getVariable(Variables.SERVICE_DELETION_ACTIONS));
    }

    @Test
    void testGetStepErrorMessage() {
        prepareServiceInstance(buildCloudServiceInstance());
        assertEquals("Error while calculating service bindings to delete \"serviceToDelete\"", step.getStepErrorMessage(context));
    }

    private CloudServiceInstanceExtended buildOptionalCloudServiceInstance() {
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .isOptional(true)
                                                    .name(SERVICE_TO_DELETE)
                                                    .v3Metadata(Metadata.builder()
                                                                        .label("label-key", "label-value")
                                                                        .build())
                                                    .build();
    }

    private CloudServiceInstanceExtended buildCloudServiceInstance() {
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .name(SERVICE_TO_DELETE)
                                                    .metadata(ImmutableCloudMetadata.of(SERVICE_INSTANCE_GUID))
                                                    .build();
    }

    private void mockDeploymentDescriptorResources() {
        DeploymentDescriptor deploymentDescriptor = DeploymentDescriptor.createV3()
                                                                        .setResources(List.of(buildOptionalExistingResource()));
        context.setVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR, deploymentDescriptor);
    }

    private Resource buildOptionalExistingResource() {
        return Resource.createV3()
                       .setName(SERVICE_TO_DELETE)
                       .setParameters(Map.of(SupportedParameters.TYPE, ResourceType.EXISTING_SERVICE.toString()))
                       .setOptional(true);
    }

    private CloudServiceBinding buildCloudServiceBinding() {
        return ImmutableCloudServiceBinding.builder()
                                           .serviceInstanceGuid(SERVICE_INSTANCE_GUID)
                                           .applicationGuid(APPLICATION_GUID)
                                           .serviceBindingOperation(buildServiceCredentialOperation())
                                           .build();
    }

    private ServiceCredentialBindingOperation buildServiceCredentialOperation() {
        return ImmutableServiceCredentialBindingOperation.builder()
                                                         .type(ServiceCredentialBindingOperation.Type.CREATE)
                                                         .state(ServiceCredentialBindingOperation.State.IN_PROGRESS)
                                                         .build();
    }

    private CloudServiceKey buildCloudServiceKey() {
        return ImmutableCloudServiceKey.builder()
                                       .name("service-key")
                                       .serviceKeyOperation(buildServiceCredentialOperation())
                                       .build();
    }

    @Override
    protected DetermineServiceDeleteActionsToExecuteStep createStep() {
        processTypeParser = mock(ProcessTypeParser.class);
        return new DetermineServiceDeleteActionsToExecuteStep(processTypeParser);
    }
}
