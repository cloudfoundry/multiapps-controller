package org.cloudfoundry.multiapps.controller.core.cf.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCfUserMetadata;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataAnnotations;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataLabels;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.api.Test;

class ApplicationMetadataBuilderTest {

    private static DeploymentDescriptor buildDescriptor() {
        DeploymentDescriptor descriptor = DeploymentDescriptor.createV3();
        descriptor.setId("test-mta");
        descriptor.setVersion("1.0.0");
        return descriptor;
    }

    private static Module buildModule() {
        return Module.createV3()
                     .setName("my-module")
                     .setType("application");
    }

    @Test
    void nullUserCfMetadataProducesOnlyMtaInternalKeys() {
        Metadata result = ApplicationMetadataBuilder.build(buildDescriptor(), null, buildModule(), List.of(), null);

        assertNotNull(result.getLabels().get(MtaMetadataLabels.MTA_ID));
        assertNotNull(result.getAnnotations().get(MtaMetadataAnnotations.MTA_ID));
    }

    @Test
    void userLabelsAndAnnotationsAreMergedAlongsideMtaInternalKeys() {
        var userMeta = ImmutableCfUserMetadata.builder()
                                              .labels(Map.of("env", "staging"))
                                              .annotations(Map.of("owner", "team-b"))
                                              .build();

        Metadata result = ApplicationMetadataBuilder.build(buildDescriptor(), null, buildModule(), List.of(), userMeta);

        assertEquals("staging", result.getLabels().get("env"));
        assertEquals("team-b", result.getAnnotations().get("owner"));
        assertNotNull(result.getLabels().get(MtaMetadataLabels.MTA_ID));
        assertNotNull(result.getAnnotations().get(MtaMetadataAnnotations.MTA_ID));
    }

    @Test
    void mtaInternalLabelsAlwaysPresentRegardlessOfUserMetadata() {
        var userMeta = ImmutableCfUserMetadata.builder()
                                              .labels(Map.of("custom", "value"))
                                              .build();

        Metadata result = ApplicationMetadataBuilder.build(buildDescriptor(), "ns1", buildModule(), List.of(), userMeta);

        assertTrue(result.getLabels().containsKey(MtaMetadataLabels.MTA_ID));
        assertTrue(result.getLabels().containsKey(MtaMetadataLabels.MTA_NAMESPACE));
        assertTrue(result.getAnnotations().containsKey(MtaMetadataAnnotations.MTA_ID));
        assertTrue(result.getAnnotations().containsKey(MtaMetadataAnnotations.MTA_NAMESPACE));
    }

}
