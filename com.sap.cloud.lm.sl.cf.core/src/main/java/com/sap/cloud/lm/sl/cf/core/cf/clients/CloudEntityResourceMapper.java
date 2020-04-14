package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.CloudEvent;
import org.cloudfoundry.client.lib.domain.CloudEvent.Participant;
import org.cloudfoundry.client.lib.domain.CloudMetadata;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.cloudfoundry.client.lib.domain.ImmutableCloudDomain;
import org.cloudfoundry.client.lib.domain.ImmutableCloudEvent;
import org.cloudfoundry.client.lib.domain.ImmutableCloudEvent.ImmutableParticipant;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableCloudRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudEntityResourceMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudEntityResourceMapper.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    public CloudRoute mapRouteResource(Map<String, Object> resource) {
        @SuppressWarnings("unchecked")
        List<Object> apps = getResourceAttribute(resource, "apps", List.class);
        String host = getResourceAttribute(resource, "host", String.class);
        String path = getResourceAttribute(resource, "path", String.class);
        boolean hasBoundService = getResourceAttribute(resource, "service_instance_guid", String.class) != null;
        CloudDomain domain = mapDomainResource(getEmbeddedResource(resource, "domain"));
        return ImmutableCloudRoute.builder()
                                  .metadata(getMetadata(resource))
                                  .host(host)
                                  .domain(domain)
                                  .path(path)
                                  .appsUsingRoute(apps.size())
                                  .hasServiceUsingRoute(hasBoundService)
                                  .build();
    }

    public CloudEvent mapEventResource(Map<String, Object> resource) {
        return ImmutableCloudEvent.builder()
                                  .metadata(getMetadata(resource))
                                  .name(getResourceAttribute(resource, "name", String.class))
                                  .actor(getActor(resource))
                                  .actee(getActee(resource))
                                  .timestamp(parseDate(getResourceAttribute(resource, "timestamp", String.class)))
                                  .type(getResourceAttribute(resource, "type", String.class))
                                  .build();
    }

    private CloudDomain mapDomainResource(Map<String, Object> resource) {
        return ImmutableCloudDomain.builder()
                                   .metadata(getMetadata(resource))
                                   .name(getResourceAttribute(resource, "name", String.class))
                                   .build();
    }

    private Participant getActor(Map<String, Object> resource) {
        UUID actorGuid = getResourceAttribute(resource, "actor", UUID.class);
        String actorType = getResourceAttribute(resource, "actor_type", String.class);
        String actorName = getResourceAttribute(resource, "actor_name", String.class);
        return ImmutableParticipant.builder()
                                   .guid(actorGuid)
                                   .name(actorName)
                                   .type(actorType)
                                   .build();
    }

    private Participant getActee(Map<String, Object> resource) {
        UUID acteeGuid = getResourceAttribute(resource, "actee", UUID.class);
        String acteeType = getResourceAttribute(resource, "actee_type", String.class);
        String acteeName = getResourceAttribute(resource, "actee_name", String.class);
        return ImmutableParticipant.builder()
                                   .guid(acteeGuid)
                                   .name(acteeName)
                                   .type(acteeType)
                                   .build();
    }

    @SuppressWarnings("unchecked")
    public static CloudMetadata getMetadata(Map<String, Object> resource) {
        Map<String, Object> metadata = (Map<String, Object>) resource.getOrDefault("metadata", Collections.emptyMap());
        UUID guid = getValue(metadata, "guid", UUID.class);
        if (guid == null) {
            return null;
        }
        return ImmutableCloudMetadata.builder()
                                     .guid(guid)
                                     .createdAt(getValue(metadata, "created_at", Date.class))
                                     .updatedAt(getValue(metadata, "updated_at", Date.class))
                                     .url(getValue(metadata, "url", String.class))
                                     .build();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getEmbeddedResource(Map<String, Object> resource, String embeddedResourceName) {
        Map<String, Object> entity = (Map<String, Object>) resource.get("entity");
        return (Map<String, Object>) entity.get(embeddedResourceName);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getResourceAttribute(Map<String, Object> resource, String attributeName, Class<T> targetClass) {
        if (resource == null) {
            return null;
        }
        Map<String, Object> entity = (Map<String, Object>) resource.get("entity");
        return getValue(entity, attributeName, targetClass);
    }

    @SuppressWarnings("unchecked")
    private static <T> T getValue(Map<String, Object> map, String key, Class<T> targetClass) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (targetClass == String.class) {
            return (T) String.valueOf(value);
        }
        if (targetClass == UUID.class && value instanceof String) {
            return (T) parseGuid((String) value);
        }
        if (targetClass == Date.class && value instanceof String) {
            return (T) parseDate((String) value);
        }
        throw new IllegalArgumentException("Error during mapping - unsupported class for attribute mapping " + targetClass.getName());
    }

    private static UUID parseGuid(String guid) {
        try {
            return UUID.fromString(guid);
        } catch (IllegalArgumentException e) {
            LOGGER.warn(MessageFormat.format("Could not parse GUID string: \"{0}\"", guid), e);
            return null;
        }
    }

    private static Date parseDate(String dateString) {
        if (dateString != null) {
            try {
                Instant instant = parseInstant(dateString);
                return Date.from(instant);
            } catch (DateTimeParseException e) {
                LOGGER.warn(MessageFormat.format("Could not parse date string: \"{0}\"", dateString), e);
            }
        }
        return null;
    }

    private static Instant parseInstant(String date) {
        String isoDate = toIsoDate(date);
        return ZonedDateTime.parse(isoDate, DATE_TIME_FORMATTER)
                            .toInstant();
    }

    private static String toIsoDate(String date) {
        // If the time zone part of the date contains a colon (e.g. 2013-09-19T21:56:36+00:00)
        // then remove it before parsing.
        return date.replaceFirst(":(?=[0-9]{2}$)", "")
                   .replaceFirst("Z$", "+0000");
    }

}
