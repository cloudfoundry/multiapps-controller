package org.cloudfoundry.multiapps.controller.process.steps;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;

class UpdateServiceMetadataStepTest extends SyncFlowableStepTest<UpdateServiceMetadataStep> {

    private static final String SERVICE_NAME = "test-service";
    private static final String METADATA_LABEL = "test-label";
    private static final String METADATA_LABEL_VALUE = "test-label-value";

    @Test
    void testUpdateServiceMetadata() {
        CloudServiceInstanceExtended serviceToProcess = buildServiceToProcess();
        prepareServiceToProcess(serviceToProcess);
        prepareClient(serviceToProcess);

        step.execute(execution);

        verify(client).updateServiceInstanceMetadata(serviceToProcess.getMetadata()
                                                                     .getGuid(),
                                                     serviceToProcess.getV3Metadata());
    }

    private CloudServiceInstanceExtended buildServiceToProcess() {
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .name(SERVICE_NAME)
                                                    .metadata(ImmutableCloudMetadata.builder()
                                                                                    .guid(UUID.randomUUID())
                                                                                    .build())
                                                    .v3Metadata(Metadata.builder()
                                                                        .label(METADATA_LABEL, METADATA_LABEL_VALUE)
                                                                        .build())
                                                    .build();
    }

    private void prepareServiceToProcess(CloudServiceInstanceExtended serviceToProcess) {
        context.setVariable(Variables.SERVICE_TO_PROCESS, serviceToProcess);
    }

    private void prepareClient(CloudServiceInstanceExtended serviceToProcess) {
        when(client.getServiceInstance(SERVICE_NAME)).thenReturn(serviceToProcess);
    }

    @Override
    protected UpdateServiceMetadataStep createStep() {
        return new UpdateServiceMetadataStep();
    }

}
