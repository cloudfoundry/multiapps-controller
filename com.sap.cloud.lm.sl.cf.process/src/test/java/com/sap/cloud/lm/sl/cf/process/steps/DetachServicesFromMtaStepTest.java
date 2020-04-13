package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.client.lib.domain.CloudMetadata;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceInstance;
import org.cloudfoundry.client.v3.Metadata;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.core.cf.metadata.MtaMetadataAnnotations;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.MtaMetadataLabels;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

public class DetachServicesFromMtaStepTest extends SyncFlowableStepTest<DetachServicesFromMtaStep> {

    static Stream<List<SimpleServiceInstance>> testExecute() {
        List<SimpleServiceInstance> servicesToDetach = Arrays.asList(createSimpleServiceInstance("service-1"),
                                                                     createSimpleServiceInstance("service-2"));
        return Stream.of(servicesToDetach);
    }

    @ParameterizedTest
    @MethodSource
    void testExecute(List<SimpleServiceInstance> servicesToDetach) {
        setUp(servicesToDetach);

        step.execute(execution);
        assertStepFinishedSuccessfully();
        validateServicesDetached(servicesToDetach);
    }

    protected void assertStepFinishedSuccessfully() {
        assertEquals(StepPhase.DONE.toString(), getExecutionStatus());
    }

    private void validateServicesDetached(List<SimpleServiceInstance> servicesToDetach) {
        for (SimpleServiceInstance serviceToDetach : servicesToDetach) {
            Mockito.verify(client)
                   .updateServiceInstanceMetadata(eq(serviceToDetach.guid), eq(getMetadataWithoutMtaFields()));
        }
    }

    private static Metadata getMetadataWithoutMtaFields() {
        return Metadata.builder()
                       .label(MtaMetadataLabels.MTA_ID, null)
                       .annotation(MtaMetadataAnnotations.MTA_ID, null)
                       .annotation(MtaMetadataAnnotations.MTA_VERSION, null)
                       .annotation(MtaMetadataAnnotations.MTA_RESOURCE, null)
                       .build();
    }

    private void setUp(List<SimpleServiceInstance> servicesToDetach) {
        List<String> serviceNamesToDetach = servicesToDetach.stream()
                                                            .map(SimpleServiceInstance::getName)
                                                            .collect(Collectors.toList());
        prepareContext(serviceNamesToDetach);
        prepareClient(servicesToDetach);
    }

    private void prepareContext(List<String> servicesToDelete) {
        context.setVariable(Variables.SERVICES_TO_DELETE, servicesToDelete);
    }

    private void prepareClient(List<SimpleServiceInstance> servicesToDelete) {
        for (SimpleServiceInstance serviceInstance : servicesToDelete) {
            Mockito.when(client.getServiceInstance(serviceInstance.name))
                   .thenReturn(createServiceInstance(serviceInstance));
        }
    }

    private CloudServiceInstance createServiceInstance(SimpleServiceInstance serviceInstance) {
        Metadata v3Metadata = Metadata.builder()
                                      .label(MtaMetadataLabels.MTA_ID, "098f6bcd4621d373cade4e832627b4f6")
                                      .annotation(MtaMetadataAnnotations.MTA_ID, "test")
                                      .annotation(MtaMetadataAnnotations.MTA_VERSION, "1")
                                      .annotation(MtaMetadataAnnotations.MTA_RESOURCE, "test")
                                      .build();
        CloudMetadata metadata = ImmutableCloudMetadata.builder()
                                                       .guid(serviceInstance.guid)
                                                       .build();
        return ImmutableCloudServiceInstance.builder()
                                            .name(serviceInstance.name)
                                            .metadata(metadata)
                                            .v3Metadata(v3Metadata)
                                            .build();
    }

    private static SimpleServiceInstance createSimpleServiceInstance(String name) {
        return new SimpleServiceInstance(name, UUID.randomUUID());
    }

    private static class SimpleServiceInstance {
        String name;
        UUID guid;

        public SimpleServiceInstance(String name, UUID guid) {
            this.name = name;
            this.guid = guid;
        }

        public String getName() {
            return this.name;
        }
    }

    @Override
    protected DetachServicesFromMtaStep createStep() {
        return new DetachServicesFromMtaStep();
    }

}
