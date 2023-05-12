package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataAnnotations;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataLabels;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaServiceKey;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMtaServiceKey;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;

class DetachServiceKeysFromMtaStepTest extends SyncFlowableStepTest<DetachServiceKeysFromMtaStep> {

    static Stream<List<DeployedMtaServiceKey>> testExecute() {
        List<DeployedMtaServiceKey> serviceKeysToDetach = List.of(createDeployedServiceKey("service-key-1", true),
                                                                  createDeployedServiceKey("service-key-2", true),
                                                                  createDeployedServiceKeyWithoutMetadata("service-key-3"));
        return Stream.of(serviceKeysToDetach);
    }

    @ParameterizedTest
    @MethodSource
    void testExecute(List<DeployedMtaServiceKey> serviceKeysToDetach) {
        setUp(serviceKeysToDetach);

        step.execute(execution);
        assertStepFinishedSuccessfully();
        validateServicesDetached(serviceKeysToDetach);
    }

    protected void assertStepFinishedSuccessfully() {
        assertEquals(StepPhase.DONE.toString(), getExecutionStatus());
    }

    private void validateServicesDetached(List<DeployedMtaServiceKey> serviceKeysToDetach) {
        for (DeployedMtaServiceKey serviceKey : serviceKeysToDetach) {
            if (serviceKey.getV3Metadata()
                          .getAnnotations()
                          .isEmpty()) {
                Mockito.verify(client, Mockito.never())
                       .updateServiceBindingMetadata(eq(serviceKey.getGuid()), eq(getMetadataWithoutMtaFields()));
            } else {
                Mockito.verify(client)
                       .updateServiceBindingMetadata(eq(serviceKey.getGuid()), eq(getMetadataWithoutMtaFields()));
            }
        }
    }

    private static Metadata getMetadataWithoutMtaFields() {
        return Metadata.builder()
                       .label(MtaMetadataLabels.MTA_ID, null)
                       .label(MtaMetadataLabels.MTA_NAMESPACE, null)
                       .label(MtaMetadataLabels.SPACE_GUID, null)
                       .annotation(MtaMetadataAnnotations.MTA_ID, null)
                       .annotation(MtaMetadataAnnotations.MTA_VERSION, null)
                       .annotation(MtaMetadataAnnotations.MTA_RESOURCE, null)
                       .annotation(MtaMetadataLabels.MTA_NAMESPACE, null)
                       .build();
    }

    private void setUp(List<DeployedMtaServiceKey> serviceKeysToDetach) {
        prepareContext(serviceKeysToDetach);
    }

    private void prepareContext(List<DeployedMtaServiceKey> serviceKeysToDelete) {
        context.setVariable(Variables.DEPLOYED_MTA_SERVICE_KEYS, serviceKeysToDelete);
    }

    private static DeployedMtaServiceKey createDeployedServiceKeyWithoutMetadata(String name) {
        return createDeployedServiceKey(name, false);
    }

    private static DeployedMtaServiceKey createDeployedServiceKey(String name, boolean generateMetadata) {
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
        return ImmutableDeployedMtaServiceKey.builder()
                                             .name(name)
                                             .metadata(metadata)
                                             .v3Metadata(v3Metadata)
                                             .build();
    }

    @Test
    void testWithMissingServiceKey() {
        DeployedMtaServiceKey missingKey = createDeployedServiceKey("service-key-missing", true);
        prepareContext(List.of(missingKey));
        Mockito.doThrow(new CloudOperationException(HttpStatus.NOT_FOUND, "testException", "key not there"))
               .when(client)
               .updateServiceBindingMetadata(eq(missingKey.getGuid()), eq(getMetadataWithoutMtaFields()));

        step.execute(execution);

        assertStepFinishedSuccessfully();
    }

    @Override
    protected DetachServiceKeysFromMtaStep createStep() {
        return new DetachServiceKeysFromMtaStep();
    }

}
