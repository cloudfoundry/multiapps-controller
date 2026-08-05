package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

import org.cloudfoundry.client.v3.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudMetadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Derivable;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudMetadata;

public abstract class RawCloudEntity<T> implements Derivable<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RawCloudEntity.class);

    protected RawCloudEntity() {
        // Recommended by Sonar.
    }

    public static CloudMetadata parseResourceMetadata(Resource resource) {
        return ImmutableCloudMetadata.builder()
                                     .guid(parseNullableGuid(resource.getId()))
                                     .createdAt(parseNullableDate(resource.getCreatedAt()))
                                     .updatedAt(parseNullableDate(resource.getUpdatedAt()))
                                     .build();
    }

    /**
     * Convert the OSS {@code LastOperation} to the project-owned {@link org.cloudfoundry.multiapps.controller.client.facade.domain.LastOperation}.
     * Shared by the service instance / binding / key adapters. (Part of the OSS-backed path; removed with the OSS client.)
     */
    protected static org.cloudfoundry.multiapps.controller.client.facade.domain.LastOperation toDomainLastOperation(
        org.cloudfoundry.client.v3.LastOperation lastOperation) {
        if (lastOperation == null) {
            return null;
        }
        return new org.cloudfoundry.multiapps.controller.client.facade.domain.LastOperation(lastOperation.getType(),
                                                                                            lastOperation.getState(),
                                                                                            lastOperation.getDescription(),
                                                                                            lastOperation.getCreatedAt(),
                                                                                            lastOperation.getUpdatedAt());
    }

    /**
     * Convert the OSS v3 {@code Metadata} to the project-owned
     * {@link org.cloudfoundry.multiapps.controller.client.facade.domain.Metadata}. (OSS-backed path; removed with the OSS client.)
     */
    protected static org.cloudfoundry.multiapps.controller.client.facade.domain.Metadata toDomainMetadata(
        org.cloudfoundry.client.v3.Metadata metadata) {
        if (metadata == null) {
            return null;
        }
        return org.cloudfoundry.multiapps.controller.client.facade.domain.Metadata.builder()
                                                                                  .labels(metadata.getLabels())
                                                                                  .annotations(metadata.getAnnotations())
                                                                                  .build();
    }

    protected static UUID parseNullableGuid(String guid) {
        return guid == null ? null : parseGuid(guid);
    }

    protected static UUID parseGuid(String guid) {
        try {
            return UUID.fromString(guid);
        } catch (IllegalArgumentException e) {
            LOGGER.warn(MessageFormat.format("Could not parse GUID string: \"{0}\"", guid), e);
            return null;
        }
    }

    protected static LocalDateTime parseNullableDate(String date) {
        return date == null ? null : parseDate(date);
    }

    protected static LocalDateTime parseDate(String dateString) {
        try {
            return ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME)
                                .toLocalDateTime();
        } catch (DateTimeParseException e) {
            LOGGER.warn(MessageFormat.format("Could not parse date string: \"{0}\"", dateString), e);
            return null;
        }
    }

    protected static <E extends Enum<E>> E parseEnum(Enum<?> value, Class<E> targetEnum) {
        String name = value.name()
                           .toUpperCase();
        return Enum.valueOf(targetEnum, name);
    }

    protected static <D> D deriveFromNullable(Derivable<D> derivable) {
        return derivable == null ? null : derivable.derive();
    }

    protected static <D> Collection<D> derive(Collection<Derivable<D>> derivables) {
        return derivables.stream()
                         .map(Derivable::derive)
                         .collect(Collectors.toList());
    }

}
