package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Thin Jackson wire-model of a CF v3 service credential binding resource ({@code GET/POST/PATCH/DELETE /v3/service_credential_bindings}). A
 * binding is either of {@code type=app} (an application bound to a service instance) or {@code type=key} (a service key); the
 * {@code relationships.app} is absent for keys.
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
public record V3ServiceBinding(@JsonProperty(V3Fields.GUID) String guid, @JsonProperty(V3Fields.NAME) String name,
                               @JsonProperty(V3Fields.TYPE) String type,
                               @JsonProperty(V3Fields.CREATED_AT) String createdAt, @JsonProperty(V3Fields.UPDATED_AT) String updatedAt,
                               @JsonProperty("last_operation") V3LastOperation lastOperation,
                               @JsonProperty(V3Fields.METADATA) V3Metadata metadata,
                               @JsonProperty(V3Fields.RELATIONSHIPS) V3Relationships relationships) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3LastOperation(@JsonProperty(V3Fields.TYPE) String type, @JsonProperty(V3Fields.STATE) String state,
                                  @JsonProperty(V3Fields.DESCRIPTION) String description,
                                  @JsonProperty(V3Fields.CREATED_AT) String createdAt,
                                  @JsonProperty(V3Fields.UPDATED_AT) String updatedAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Relationships(@JsonProperty(V3Fields.APP) V3ToOneRelationship application,
                                  @JsonProperty(V3Fields.SERVICE_INSTANCE) V3ToOneRelationship serviceInstance) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3ToOneRelationship(@JsonProperty(V3Fields.DATA) V3RelationshipData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3RelationshipData(@JsonProperty(V3Fields.GUID) String guid) {
    }

}
