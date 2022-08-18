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

import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.client.v3.serviceinstances.ServiceInstanceType;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaService;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaServiceKey;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMtaServiceKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloudfoundry.client.facade.domain.CloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceInstance;

public class CloudEntityResourceMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudEntityResourceMapper.class);
    
    @SuppressWarnings("unchecked")
    public String getRelatedObjectGuid(Map<String, Object> resource, String relationshipName) {
        var relationships = (Map<String, Object>) resource.get("relationships");
        return Optional.ofNullable((Map<String, Object>) relationships.get(relationshipName))
                       .map(relationshipsJson -> (Map<String, Object>) relationshipsJson.get("data"))
                       .map(relationshipData -> (String) relationshipData.get("guid"))
                       .orElse(null);
    }

    public CloudServiceInstance mapService(Map<String, Object> resource) {
        return ImmutableCloudServiceInstance.builder()
                                            .metadata(getCloudMetadataV3(resource))
                                            .v3Metadata(getMetadataV3(resource))
                                            .name(getValue(resource, "name", String.class))
                                            .type(ServiceInstanceType.from((String) resource.get("type")))
                                            .build();
    }

    public DeployedMtaServiceKey mapServiceKeyResource(Map<String, Object> resource, Map<String, CloudServiceInstance> mtaServices) {
        String serviceInstanceGuid = getRelatedObjectGuid(resource, "service_instance");
        return ImmutableDeployedMtaServiceKey.builder()
                                             .metadata(getCloudMetadataV3(resource))
                                             .v3Metadata(getMetadataV3(resource))
                                             .name(getValue(resource, "name", String.class))
                                             .serviceInstance(mtaServices.get(serviceInstanceGuid))
                                             .resourceName(getResourceNameForCloudService(mtaServices.get(serviceInstanceGuid)))
                                             .build();
    }

    private String getResourceNameForCloudService(CloudServiceInstance service) {
        if (service instanceof DeployedMtaService) {
            return ((DeployedMtaService) service).getResourceName();
        }
        return null;
    }

    public static CloudMetadata getCloudMetadataV3(Map<String, Object> resource) {
        UUID guid = getValue(resource, "guid", UUID.class);
        if (guid == null) {
            return null;
        }
        return ImmutableCloudMetadata.builder()
                                     .guid(guid)
                                     .createdAt(getValue(resource, "created_at", LocalDateTime.class))
                                     .updatedAt(getValue(resource, "updated_at", LocalDateTime.class))
                                     .build();
    }

    @SuppressWarnings("unchecked")
    public static Metadata getMetadataV3(Map<String, Object> resource) {
        Map<String, Object> metadata = (Map<String, Object>) resource.getOrDefault("metadata", Collections.emptyMap());
        return Metadata.builder()
                       .labels((Map<String, String>) metadata.getOrDefault("labels", Collections.emptyMap()))
                       .annotations((Map<String, String>) metadata.getOrDefault("annotations", Collections.emptyMap()))
                       .build();
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
