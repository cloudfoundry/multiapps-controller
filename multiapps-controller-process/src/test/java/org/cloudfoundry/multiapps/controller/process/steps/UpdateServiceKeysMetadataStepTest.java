package org.cloudfoundry.multiapps.controller.process.steps;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataAnnotations;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataLabels;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceKey;

class UpdateServiceKeysMetadataStepTest extends SyncFlowableStepTest<UpdateServiceKeysMetadataStep> {

    private static int TEST_KEY_COUNT = 0;

    @Test
    void testUpdateServiceMetadata() {
        List<CloudServiceKey> testKeys = Arrays.asList(buildTestServiceKey(true), buildTestServiceKey(true));
        prepareServiceKeysToUpdate(testKeys);

        step.execute(execution);

        for (var key : testKeys) {
            verify(client).updateServiceBindingMetadata(key.getMetadata()
                                                           .getGuid(),
                                                        key.getV3Metadata());
        }

    }

    @Test
    void testUpdateServiceMetadataFailure() {
        List<CloudServiceKey> testKeys = Arrays.asList(buildTestServiceKey(true), buildTestServiceKey(true), buildTestServiceKey(true));
        prepareServiceKeysToUpdate(testKeys);
        CloudServiceKey brokenKey = testKeys.get(1);
        doThrow(new CloudOperationException(HttpStatus.FORBIDDEN, "Test")).when(client)
                                                                          .updateServiceBindingMetadata(Mockito.eq(brokenKey.getGuid()),
                                                                                                        Mockito.eq(brokenKey.getV3Metadata()));

        step.execute(execution);

        for (var key : testKeys) {
            verify(client).updateServiceBindingMetadata(key.getMetadata()
                                                           .getGuid(),
                                                        key.getV3Metadata());
        }
        verify(stepLogger).errorWithoutProgressMessage(Mockito.any(CloudOperationException.class),
                                                       Mockito.eq(Messages.UPDATING_SERVICE_KEY_0_METADATA_FAILED),
                                                       Mockito.eq(brokenKey.getName()));
    }

    private CloudServiceKey buildTestServiceKey(boolean generateMetadata) {
        Metadata v3Metadata;
        if (generateMetadata) {
            v3Metadata = Metadata.builder()
                                 .label(MtaMetadataLabels.MTA_ID, "098f6bcd4621d373cade4e832627b4f6")
                                 .annotation(MtaMetadataAnnotations.MTA_ID, "test")
                                 .annotation(MtaMetadataAnnotations.MTA_VERSION, "1")
                                 .annotation(MtaMetadataAnnotations.MTA_RESOURCE, "test")
                                 .build();
        } else {
            v3Metadata = Metadata.builder()
                                 .labels(Collections.emptyMap())
                                 .annotations(Collections.emptyMap())
                                 .build();
        }
        CloudMetadata metadata = ImmutableCloudMetadata.builder()
                                                       .guid(UUID.randomUUID())
                                                       .build();
        return ImmutableCloudServiceKey.builder()
                                       .name("testKey_" + (++TEST_KEY_COUNT))
                                       .metadata(metadata)
                                       .v3Metadata(v3Metadata)
                                       .build();
    }

    private void prepareServiceKeysToUpdate(List<CloudServiceKey> serviceKeys) {
        context.setVariable(Variables.CLOUD_SERVICE_KEYS_TO_UPDATE_METADATA, serviceKeys);
    }

    @Override
    protected UpdateServiceKeysMetadataStep createStep() {
        return new UpdateServiceKeysMetadataStep();
    }

}
