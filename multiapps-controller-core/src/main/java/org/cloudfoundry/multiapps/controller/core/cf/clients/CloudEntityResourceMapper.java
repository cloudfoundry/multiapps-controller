package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudRouteExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudRouteExtended;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloudfoundry.client.facade.domain.CloudDomain;
import com.sap.cloudfoundry.client.facade.domain.CloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudDomain;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;

public class CloudEntityResourceMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudEntityResourceMapper.class);

    @SuppressWarnings("unchecked")
    public String getRelationshipData(Map<String, Object> resource, String relationship) {
        var relationships = (Map<String, Object>) resource.get("relationships");
        return Optional.ofNullable((Map<String, Object>) relationships.get(relationship))
                       .map(relationshipsJson -> (Map<String, Object>) relationshipsJson.get("data"))
                       .map(relationshipData -> (String) relationshipData.get("guid"))
                       .orElse("");
    }

    public CloudRouteExtended mapRouteResource(Map<String, Object> routeResource, Map<String, Object> domainResource,
                                               List<Map<String, Object>> serviceRouteBindings) {
        @SuppressWarnings("unchecked")
        List<Object> destinations = getValue(routeResource, "destinations", List.class);
        return ImmutableCloudRouteExtended.builder()
                                          .metadata(getMetadata(routeResource))
                                          .host(getValue(routeResource, "host", String.class))
                                          .domain(mapDomainResource(domainResource))
                                          .path(getValue(routeResource, "path", String.class))
                                          .appsUsingRoute(destinations.size())
                                          .serviceRouteBindings(mapServiceRouteBindingResources(serviceRouteBindings))
                                          .build();
    }

    private CloudDomain mapDomainResource(Map<String, Object> resource) {
        return ImmutableCloudDomain.builder()
                                   .metadata(getMetadata(resource))
                                   .name(getValue(resource, "name", String.class))
                                   .build();
    }

    private CloudMetadata getMetadata(Map<String, Object> resource) {
        return ImmutableCloudMetadata.builder()
                                     .guid(getValue(resource, "guid", UUID.class))
                                     .createdAt(getValue(resource, "created_at", LocalDateTime.class))
                                     .updatedAt(getValue(resource, "updated_at", LocalDateTime.class))
                                     .build();
    }

    private List<String> mapServiceRouteBindingResources(List<Map<String, Object>> serviceRouteBindings) {
        if (serviceRouteBindings == null) {
            return Collections.emptyList();
        }
        return serviceRouteBindings.stream()
                                   .map(serviceRouteBindingResource -> (String) serviceRouteBindingResource.get("guid"))
                                   .collect(Collectors.toList());
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
        if (targetClass == List.class) {
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
