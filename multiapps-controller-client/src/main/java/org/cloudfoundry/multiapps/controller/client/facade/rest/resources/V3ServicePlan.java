package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Thin Jackson wire-model of a CF v3 service plan resource ({@code GET /v3/service_plans}).
 *
 * <pre>
 * { "guid": "...", "name": "...", "description": "...", "free": true|false,
 *   "visibility_type": "public|admin|organization|space",
 *   "created_at": "...", "updated_at": "...",
 *   "broker_catalog": { "id": "...", "metadata": { ... } },
 *   "relationships": { "service_offering": { "data": { "guid": "..." } } },
 *   "metadata": { "labels": {...}, "annotations": {...} } }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record V3ServicePlan(@JsonProperty(V3Fields.GUID) String guid, @JsonProperty(V3Fields.NAME) String name,
                            @JsonProperty(V3Fields.DESCRIPTION) String description, @JsonProperty("free") Boolean free,
                            @JsonProperty("visibility_type") String visibilityType, @JsonProperty(V3Fields.CREATED_AT) String createdAt,
                            @JsonProperty(V3Fields.UPDATED_AT) String updatedAt, @JsonProperty("broker_catalog") V3BrokerCatalog brokerCatalog,
                            @JsonProperty(V3Fields.RELATIONSHIPS) V3Relationships relationships, @JsonProperty(V3Fields.METADATA) V3Metadata metadata) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3BrokerCatalog(@JsonProperty("id") String id, @JsonProperty(V3Fields.METADATA) Map<String, Object> metadata) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Relationships(@JsonProperty("service_offering") V3ToOneRelationship serviceOffering) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3ToOneRelationship(@JsonProperty(V3Fields.DATA) V3RelationshipData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3RelationshipData(@JsonProperty(V3Fields.GUID) String guid) {
    }

}
