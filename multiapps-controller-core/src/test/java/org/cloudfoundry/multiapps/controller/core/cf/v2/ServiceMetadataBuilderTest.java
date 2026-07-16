package org.cloudfoundry.multiapps.controller.core.cf.v2;

import java.util.Map;

import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCfUserMetadata;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataAnnotations;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataLabels;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceMetadataBuilderTest {

    private static DeploymentDescriptor buildDescriptor() {
        DeploymentDescriptor descriptor = DeploymentDescriptor.createV3();
        descriptor.setId("test-mta");
        descriptor.setVersion("1.0.0");
        return descriptor;
    }

    private static Resource buildResource() {
        return Resource.createV3()
                       .setName("my-resource");
    }

    @Test
    void nullUserCfMetadataProducesOnlyMtaInternalKeys() {
        Metadata result = ServiceMetadataBuilder.build(buildDescriptor(), null, buildResource(), null);

        assertNotNull(result.getLabels()
                            .get(MtaMetadataLabels.MTA_ID));
        assertNotNull(result.getAnnotations()
                            .get(MtaMetadataAnnotations.MTA_ID));
        assertNotNull(result.getAnnotations()
                            .get(MtaMetadataAnnotations.MTA_RESOURCE));
    }

    @Test
    void userLabelsAndAnnotationsAreMergedAlongsideMtaInternalKeys() {
        var userMeta = ImmutableCfUserMetadata.builder()
                                              .labels(Map.of("env", "staging"))
                                              .annotations(Map.of("owner", "team-b"))
                                              .build();

        Metadata result = ServiceMetadataBuilder.build(buildDescriptor(), null, buildResource(), userMeta);

        assertEquals("staging", result.getLabels()
                                      .get("env"));
        assertEquals("team-b", result.getAnnotations()
                                     .get("owner"));
        assertNotNull(result.getLabels()
                            .get(MtaMetadataLabels.MTA_ID));
        assertNotNull(result.getAnnotations()
                            .get(MtaMetadataAnnotations.MTA_RESOURCE));
    }

    @Test
    void mtaInternalKeysAlwaysPresentRegardlessOfUserMetadata() {
        var userMeta = ImmutableCfUserMetadata.builder()
                                              .labels(Map.of("custom", "value"))
                                              .build();

        Metadata result = ServiceMetadataBuilder.build(buildDescriptor(), "ns1", buildResource(), userMeta);

        assertTrue(result.getLabels()
                         .containsKey(MtaMetadataLabels.MTA_ID));
        assertTrue(result.getLabels()
                         .containsKey(MtaMetadataLabels.MTA_NAMESPACE));
        assertTrue(result.getAnnotations()
                         .containsKey(MtaMetadataAnnotations.MTA_RESOURCE));
    }

    @Test
    void emptyUserCfMetadataLeavesOnlyMtaInternalKeys() {
        var userMeta = ImmutableCfUserMetadata.builder()
                                              .build();

        Metadata result = ServiceMetadataBuilder.build(buildDescriptor(), null, buildResource(), userMeta);

        assertNotNull(result.getAnnotations()
                            .get(MtaMetadataAnnotations.MTA_RESOURCE));
        assertEquals(result.getAnnotations()
                           .get(MtaMetadataAnnotations.MTA_RESOURCE),
                     ServiceMetadataBuilder.build(buildDescriptor(), null, buildResource(), null)
                                           .getAnnotations()
                                           .get(MtaMetadataAnnotations.MTA_RESOURCE));
    }

}
