package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudMetadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudMetadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            LOGGER.warn(MessageFormat.format(Messages.COULD_NOT_PARSE_GUID_STRING, guid), e);
            return null;
        }
    }

    public static LocalDateTime parseNullableDate(String date) {
        if (date == null) {
            return null;
        }

        try {
            return OffsetDateTime.parse(date)
                                 .toLocalDateTime();
        } catch (DateTimeParseException e) {
            LOGGER.warn(MessageFormat.format(Messages.COULD_NOT_PARSE_DATE_STRING, date), e);
            return null;
        }
    }

}
