package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloudfoundry.client.facade.domain.CloudDomain;
import com.sap.cloudfoundry.client.facade.domain.CloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.CloudRoute;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudDomain;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudRoute;

public class CloudEntityResourceMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudEntityResourceMapper.class);

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
                                  .appsUsingRoute(CollectionUtils.size(apps))
                                  .hasServiceUsingRoute(hasBoundService)
                                  .build();
    }

    private CloudDomain mapDomainResource(Map<String, Object> resource) {
        return ImmutableCloudDomain.builder()
                                   .metadata(getMetadata(resource))
                                   .name(getResourceAttribute(resource, "name", String.class))
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
                                     .createdAt(getValue(metadata, "created_at", LocalDateTime.class))
                                     .updatedAt(getValue(metadata, "updated_at", LocalDateTime.class))
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
        if (targetClass == Integer.class || targetClass == Boolean.class || targetClass == Map.class || targetClass == List.class) {
            return (T) value;
        }
        if (targetClass == UUID.class && value instanceof String) {
            return (T) parseGuid((String) value);
        }
        if (targetClass == LocalDateTime.class && value instanceof String) {
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

    private static LocalDateTime parseDate(String dateString) {
        try {
            return ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME)
                                .toLocalDateTime();
        } catch (DateTimeParseException e) {
            LOGGER.warn(MessageFormat.format("Could not parse date string: \"{0}\"", dateString), e);
            return null;
        }
    }

}
