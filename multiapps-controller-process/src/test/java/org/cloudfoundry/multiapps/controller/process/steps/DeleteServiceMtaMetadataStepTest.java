package org.cloudfoundry.multiapps.controller.process.steps;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;

import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;

class DeleteServiceMtaMetadataStepTest extends SyncFlowableStepTest<DeleteServiceMtaMetadataStep> {

    private static final String SERVICE_INSTANCE_NAME = "test-service-instance";
    private static final UUID SERVICE_INSTANCE_GUID = UUID.randomUUID();

    @Test
    void testDeleteServiceMetadata() {
        context.setVariable(Variables.SERVICE_TO_DELETE, SERVICE_INSTANCE_NAME);
        CloudServiceInstanceExtended serviceInstanceToDelete = buildCloudServiceInstanceExtended();
        when(client.getServiceInstanceWithoutAuxiliaryContent(SERVICE_INSTANCE_NAME)).thenReturn(serviceInstanceToDelete);
        step.execute(execution);
        assertStepFinishedSuccessfully();
        verify(client).updateServiceInstanceMetadata(eq(SERVICE_INSTANCE_GUID), any());
    }

    private CloudServiceInstanceExtended buildCloudServiceInstanceExtended() {
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .name(SERVICE_INSTANCE_NAME)
                                                    .metadata(ImmutableCloudMetadata.of(SERVICE_INSTANCE_GUID))
                                                    .v3Metadata(Metadata.builder()
                                                                        .annotation("key", "value")
                                                                        .build())
                                                    .build();
    }

    @Override
    protected DeleteServiceMtaMetadataStep createStep() {
        return new DeleteServiceMtaMetadataStep();
    }
}
