package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Thin Jackson wire-model of a CF v3 task resource ({@code GET/POST /v3/tasks}, {@code POST /v3/apps/{guid}/tasks}).
 *
 * <pre>
 * { "guid": "...", "name": "...", "command": "...", "state": "PENDING|RUNNING|SUCCEEDED|CANCELING|FAILED",
 *   "memory_in_mb": 256, "disk_in_mb": 1024,
 *   "result": { "failure_reason": "..." | null },
 *   "created_at": "...", "updated_at": "...",
 *   "relationships": {
 *     "app": {
 *       "data": {
 *         "guid": "ccc25a0f-c8f4-4b39-9f1b-de9f328d0ee5"
 *       }
 *     }
 *   },
 *   "metadata": { "labels": {...}, "annotations": {...} } }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record V3Task(@JsonProperty(V3Fields.GUID) String guid, @JsonProperty(V3Fields.NAME) String name,
                     @JsonProperty(V3Fields.COMMAND) String command,
                     @JsonProperty(V3Fields.STATE) String state, @JsonProperty("memory_in_mb") Integer memoryInMb,
                     @JsonProperty("disk_in_mb") Integer diskInMb, @JsonProperty("result") V3Result result,
                     @JsonProperty(V3Fields.CREATED_AT) String createdAt, @JsonProperty(V3Fields.UPDATED_AT) String updatedAt,
                     @JsonProperty(V3Fields.RELATIONSHIPS) V3TaskRelationships relationships,
                     @JsonProperty(V3Fields.METADATA) V3Metadata metadata) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Result(@JsonProperty("failure_reason") String failureReason) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3TaskRelationships(@JsonProperty(V3Fields.APP) V3Task.V3ToOne app) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3ToOne(@JsonProperty(V3Fields.DATA) V3Task.V3Data data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Data(@JsonProperty(V3Fields.GUID) String guid) {
    }

}
