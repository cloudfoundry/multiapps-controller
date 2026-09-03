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
public record V3ServiceBroker(@JsonProperty("guid") String guid, @JsonProperty("name") String name, @JsonProperty("url") String url,
                              @JsonProperty("created_at") String createdAt, @JsonProperty("updated_at") String updatedAt,
                              @JsonProperty("metadata") V3Metadata metadata,
                              @JsonProperty("relationships") V3Relationships relationships) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Relationships(@JsonProperty("space") V3ToOneRelationship space) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3ToOneRelationship(@JsonProperty("data") V3RelationshipData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3RelationshipData(@JsonProperty("guid") String guid) {
    }

}
