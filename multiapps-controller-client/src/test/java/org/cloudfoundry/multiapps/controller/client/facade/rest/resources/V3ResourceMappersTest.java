package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudMetadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Metadata;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class V3ResourceMappersTest {

    private static final String GUID_STRING = "3725a721-6e3f-4b6e-8f2b-1c2d3e4f5a6b";
    private static final UUID GUID = UUID.fromString(GUID_STRING);
    private static final String CREATED_AT_STRING = "2026-08-04T10:15:30Z";
    private static final String UPDATED_AT_STRING = "2026-08-05T11:16:31Z";

    @Test
    void testParseMetadataPopulatesAllFields() {
        CloudMetadata metadata = V3ResourceMappers.parseMetadata(GUID_STRING, CREATED_AT_STRING, UPDATED_AT_STRING);

        Assertions.assertEquals(GUID, metadata.getGuid());
        Assertions.assertEquals(LocalDateTime.of(2026, 8, 4, 10, 15, 30), metadata.getCreatedAt());
        Assertions.assertEquals(LocalDateTime.of(2026, 8, 5, 11, 16, 31), metadata.getUpdatedAt());
    }

    @Test
    void testParseMetadataWithAllNullsReturnsEmptyMetadata() {
        CloudMetadata metadata = V3ResourceMappers.parseMetadata(null, null, null);

        Assertions.assertNull(metadata.getGuid());
        Assertions.assertNull(metadata.getCreatedAt());
        Assertions.assertNull(metadata.getUpdatedAt());
    }

    @Test
    void testParseNullableGuidWithValidGuid() {
        Assertions.assertEquals(GUID, V3ResourceMappers.parseNullableGuid(GUID_STRING));
    }

    @Test
    void testParseNullableGuidWithNullReturnsNull() {
        Assertions.assertNull(V3ResourceMappers.parseNullableGuid(null));
    }

    @Test
    void testParseNullableGuidWithMalformedStringReturnsNull() {
        Assertions.assertNull(V3ResourceMappers.parseNullableGuid("not-a-guid"));
    }

    @Test
    void testParseNullableDateWithValidOffsetDateTime() {
        Assertions.assertEquals(LocalDateTime.of(2026, 8, 4, 10, 15, 30), V3ResourceMappers.parseNullableDate(CREATED_AT_STRING));
    }

    @Test
    void testParseNullableDateWithNullReturnsNull() {
        Assertions.assertNull(V3ResourceMappers.parseNullableDate(null));
    }

    @Test
    void testParseNullableDateWithMalformedStringReturnsNull() {
        Assertions.assertNull(V3ResourceMappers.parseNullableDate("2026/08/04 10:15"));
    }

    @Test
    void testToV3MetadataWithNullReturnsNull() {
        Assertions.assertNull(V3ResourceMappers.toV3Metadata(null));
    }

    @Test
    void testToV3MetadataCopiesLabelsAndAnnotations() {
        V3Metadata wire = new V3Metadata(Map.of("label-key", "label-value"), Map.of("annotation-key", "annotation-value"));

        Metadata metadata = V3ResourceMappers.toV3Metadata(wire);

        Assertions.assertEquals(Map.of("label-key", "label-value"), metadata.getLabels());
        Assertions.assertEquals(Map.of("annotation-key", "annotation-value"), metadata.getAnnotations());
    }

    @Test
    void testToV3MetadataWithNullMapsReturnsEmptyMaps() {
        V3Metadata wire = new V3Metadata(null, null);

        Metadata metadata = V3ResourceMappers.toV3Metadata(wire);

        Assertions.assertTrue(metadata.getLabels()
                                      .isEmpty());
        Assertions.assertTrue(metadata.getAnnotations()
                                      .isEmpty());
    }

}
