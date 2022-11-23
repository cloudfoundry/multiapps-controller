package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;

import com.sap.cloudfoundry.client.facade.domain.CloudServiceBinding;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceBinding;

class DetermineServiceBindingsToDeleteStepTest extends SyncFlowableStepTest<DetermineServiceBindingsToDeleteStep> {

    private static final String SERVICE_INSTANCE_NAME = "test-service-instance";
    private static final String APP_NAME = "chuck-berry";
    private static final UUID SERVICE_INSTANCE_GUID = UUID.randomUUID();
    private static final UUID APP_GUID = UUID.randomUUID();

    @Test
    void testStepExecution() {
        context.setVariable(Variables.APP_TO_PROCESS, buildCloudApplicationExtended());
        List<CloudServiceBinding> serviceBindings = List.of(buildCloudServiceBinding());
        when(client.getAppBindings(APP_GUID)).thenReturn(serviceBindings);
        when(client.getApplicationGuid(APP_NAME)).thenReturn(APP_GUID);
        when(client.getRequiredServiceInstanceGuid(SERVICE_INSTANCE_NAME)).thenReturn(SERVICE_INSTANCE_GUID);
        step.execute(execution);
        assertStepFinishedSuccessfully();
        assertEquals(serviceBindings, context.getVariable(Variables.CLOUD_SERVICE_BINDINGS_TO_DELETE));
    }

    private CloudApplicationExtended buildCloudApplicationExtended() {
        return ImmutableCloudApplicationExtended.builder()
                                                .name(APP_NAME)
                                                .metadata(ImmutableCloudMetadata.of(APP_GUID))
                                                .build();
    }

    private CloudServiceBinding buildCloudServiceBinding() {
        return ImmutableCloudServiceBinding.builder()
                                           .name(SERVICE_INSTANCE_NAME)
                                           .serviceInstanceGuid(SERVICE_INSTANCE_GUID)
                                           .applicationGuid(APP_GUID)
                                           .build();
    }

    @Override
    protected DetermineServiceBindingsToDeleteStep createStep() {
        return new DetermineServiceBindingsToDeleteStep();
    }
}
