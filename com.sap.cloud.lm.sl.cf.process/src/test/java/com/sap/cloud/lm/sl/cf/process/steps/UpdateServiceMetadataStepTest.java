package com.sap.cloud.lm.sl.cf.process.steps;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.v3.Metadata;
import org.junit.jupiter.api.Test;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudServiceExtended;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

public class UpdateServiceMetadataStepTest extends SyncFlowableStepTest<UpdateServiceMetadataStep> {

    private static final String SERVICE_NAME = "test-service";
    private static final String METADATA_LABEL = "test-label";
    private static final String METADATA_LABEL_VALUE = "test-label-value";

    @Test
    public void testUpdateServiceMetadata() {
        CloudServiceExtended serviceToProcess = buildServiceToProcess();
        prepareServiceToProcess(serviceToProcess);
        prepareClient(serviceToProcess);

        step.execute(context);

        verify(client).updateServiceMetadata(serviceToProcess.getMetadata()
                                                             .getGuid(),
                                             serviceToProcess.getV3Metadata());
    }

    private CloudServiceExtended buildServiceToProcess() {
        return ImmutableCloudServiceExtended.builder()
                                            .name(SERVICE_NAME)
                                            .metadata(ImmutableCloudMetadata.builder()
                                                                            .guid(UUID.randomUUID())
                                                                            .build())
                                            .v3Metadata(Metadata.builder()
                                                                .label(METADATA_LABEL, METADATA_LABEL_VALUE)
                                                                .build())
                                            .build();
    }

    private void prepareServiceToProcess(CloudServiceExtended serviceToProcess) {
        execution.setVariable(Variables.SERVICE_TO_PROCESS, serviceToProcess);
    }

    private void prepareClient(CloudServiceExtended serviceToProcess) {
        when(client.getService(SERVICE_NAME)).thenReturn(serviceToProcess);
    }

    @Override
    protected UpdateServiceMetadataStep createStep() {
        return new UpdateServiceMetadataStep();
    }

}
