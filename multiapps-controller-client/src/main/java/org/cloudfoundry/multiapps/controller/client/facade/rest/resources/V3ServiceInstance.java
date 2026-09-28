package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Thin Jackson wire-model of a CF v3 service instance resource ({@code GET/POST/PATCH /v3/service_instances}).
 *
 * <pre>
 * { "guid": "...", "name": "...", "type": "managed|user-provided",
 *   "created_at": "...", "updated_at": "...",
 *   "tags": [...], "syslog_drain_url": "...",
 *   "last_operation": { "type": "create|update|delete", "state": "succeeded|failed|in progress|initial", "description": "..." },
 *   "metadata": { "labels": {...}, "annotations": {...} },
 *   "relationships": { "space": { "data": { "guid": "..." } }, "service_plan": { "data": { "guid": "..." } } } }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record V3ServiceInstance(@JsonProperty(V3Fields.GUID) String guid, @JsonProperty(V3Fields.NAME) String name,
                                @JsonProperty(V3Fields.TYPE) String type,
                                @JsonProperty(V3Fields.CREATED_AT) String createdAt, @JsonProperty(V3Fields.UPDATED_AT) String updatedAt,
                                @JsonProperty("tags") List<String> tags, @JsonProperty("syslog_drain_url") String syslogDrainUrl,
                                @JsonProperty("last_operation") V3LastOperation lastOperation,
                                @JsonProperty(V3Fields.METADATA) V3Metadata metadata,
                                @JsonProperty(V3Fields.RELATIONSHIPS) V3Relationships relationships) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3LastOperation(@JsonProperty(V3Fields.TYPE) String type, @JsonProperty(V3Fields.STATE) String state,
                                  @JsonProperty(V3Fields.DESCRIPTION) String description) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Relationships(@JsonProperty(V3Fields.SPACE) V3ToOneRelationship space,
                                  @JsonProperty("service_plan") V3ToOneRelationship servicePlan) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3ToOneRelationship(@JsonProperty(V3Fields.DATA) V3RelationshipData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3RelationshipData(@JsonProperty(V3Fields.GUID) String guid) {
    }

}
