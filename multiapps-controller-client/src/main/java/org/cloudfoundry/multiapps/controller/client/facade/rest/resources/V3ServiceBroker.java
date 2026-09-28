package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Thin Jackson wire-model of a CF v3 service broker resource ({@code GET/POST/PATCH /v3/service_brokers}).
 *
 * <pre>
 * { "guid": "...", "name": "...", "url": "...", "created_at": "...", "updated_at": "...",
 *   "metadata": { "labels": {...}, "annotations": {...} },
 *   "relationships": { "space": { "data": { "guid": "..." } | null } } }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record V3ServiceBroker(@JsonProperty(V3Fields.GUID) String guid, @JsonProperty(V3Fields.NAME) String name,
                              @JsonProperty("url") String url,
                              @JsonProperty(V3Fields.CREATED_AT) String createdAt, @JsonProperty(V3Fields.UPDATED_AT) String updatedAt,
                              @JsonProperty(V3Fields.METADATA) V3Metadata metadata,
                              @JsonProperty(V3Fields.RELATIONSHIPS) V3Relationships relationships) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Relationships(@JsonProperty(V3Fields.SPACE) V3ToOneRelationship space) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3ToOneRelationship(@JsonProperty(V3Fields.DATA) V3RelationshipData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3RelationshipData(@JsonProperty(V3Fields.GUID) String guid) {
    }

}
