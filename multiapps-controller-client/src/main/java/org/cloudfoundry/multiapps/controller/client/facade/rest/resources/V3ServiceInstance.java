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
 *
 * Only the fields the client needs are mapped; everything else is ignored so CF can evolve the payload without breaking us.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record V3ServiceInstance(@JsonProperty("guid") String guid, @JsonProperty("name") String name, @JsonProperty("type") String type,
                                @JsonProperty("created_at") String createdAt, @JsonProperty("updated_at") String updatedAt,
                                @JsonProperty("tags") List<String> tags, @JsonProperty("syslog_drain_url") String syslogDrainUrl,
                                @JsonProperty("last_operation") V3LastOperation lastOperation, @JsonProperty("metadata") V3Metadata metadata,
                                @JsonProperty("relationships") V3Relationships relationships) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3LastOperation(@JsonProperty("type") String type, @JsonProperty("state") String state,
                                  @JsonProperty("description") String description) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Relationships(@JsonProperty("space") V3ToOneRelationship space,
                                  @JsonProperty("service_plan") V3ToOneRelationship servicePlan) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3ToOneRelationship(@JsonProperty("data") V3RelationshipData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3RelationshipData(@JsonProperty("guid") String guid) {
    }

}
