package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Thin Jackson wire-model of a CF v3 service credential binding resource ({@code GET/POST/PATCH/DELETE
 * /v3/service_credential_bindings}). A binding is either of {@code type=app} (an application bound to a service instance) or
 * {@code type=key} (a service key); the {@code relationships.app} is absent for keys.
 *
 * <pre>
 * { "guid": "...", "name": "...", "type": "app|key", "created_at": "...", "updated_at": "...",
 *   "last_operation": { "type": "create|delete", "state": "initial|in progress|succeeded|failed",
 *                       "description": "...", "created_at": "...", "updated_at": "..." },
 *   "metadata": { "labels": {...}, "annotations": {...} },
 *   "relationships": { "app": { "data": { "guid": "..." } | null },
 *                      "service_instance": { "data": { "guid": "..." } } } }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record V3ServiceBinding(@JsonProperty("guid") String guid, @JsonProperty("name") String name, @JsonProperty("type") String type,
                               @JsonProperty("created_at") String createdAt, @JsonProperty("updated_at") String updatedAt,
                               @JsonProperty("last_operation") V3LastOperation lastOperation,
                               @JsonProperty("metadata") V3Metadata metadata,
                               @JsonProperty("relationships") V3Relationships relationships) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3LastOperation(@JsonProperty("type") String type, @JsonProperty("state") String state,
                                  @JsonProperty("description") String description, @JsonProperty("created_at") String createdAt,
                                  @JsonProperty("updated_at") String updatedAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Relationships(@JsonProperty("app") V3ToOneRelationship application,
                                  @JsonProperty("service_instance") V3ToOneRelationship serviceInstance) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3ToOneRelationship(@JsonProperty("data") V3RelationshipData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3RelationshipData(@JsonProperty("guid") String guid) {
    }

}
