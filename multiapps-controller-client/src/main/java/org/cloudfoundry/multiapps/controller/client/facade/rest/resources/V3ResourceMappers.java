package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.domain.Metadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudMetadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helpers shared by all CF v3 wire-model mappers: parse the resource envelope fields ({@code guid}, {@code created_at},
 * {@code updated_at}) into the project's {@link CloudMetadata}, and parse GUIDs/dates leniently — mirroring the behaviour of the OSS
 * {@code RawCloudEntity} so the two client implementations produce identical domain objects.
 */
public final class V3ResourceMappers {

    private static final Logger LOGGER = LoggerFactory.getLogger(V3ResourceMappers.class);

    private V3ResourceMappers() {
    }

    public static CloudMetadata parseMetadata(String guid, String createdAt, String updatedAt) {
        return ImmutableCloudMetadata.builder()
                                     .guid(parseNullableGuid(guid))
                                     .createdAt(parseNullableDate(createdAt))
                                     .updatedAt(parseNullableDate(updatedAt))
                                     .build();
    }

    /**
     * Build the project's v3 {@link Metadata} (labels/annotations) from the wire representation. Kept as the OSS leaf type for now; it
     * is re-homed to a project-owned type in the leaf-type migration step.
     */
    public static Metadata toV3Metadata(V3Metadata metadata) {
        if (metadata == null) {
            return null;
        }
        return Metadata.builder()
                       .labels(metadata.labels())
                       .annotations(metadata.annotations())
                       .build();
    }

    public static UUID parseNullableGuid(String guid) {
        if (guid == null) {
            return null;
        }
        try {
            return UUID.fromString(guid);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Could not parse GUID string: \"{}\"", guid, e);
            return null;
        }
    }

    public static LocalDateTime parseNullableDate(String date) {
        if (date == null) {
            return null;
        }
        try {
            // CF v3 timestamps are ISO-8601 with offset, e.g. 2026-08-04T10:15:30Z
            return java.time.OffsetDateTime.parse(date)
                                           .toLocalDateTime();
        } catch (DateTimeParseException e) {
            LOGGER.warn("Could not parse date string: \"{}\"", date, e);
            return null;
        }
    }

}
