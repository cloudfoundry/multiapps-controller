package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Thin Jackson wire-model of a CF v3 application resource ({@code GET/POST /v3/apps}).
 *
 * <pre>
 * { "guid": "...", "name": "...", "state": "STARTED|STOPPED", "created_at": "...", "updated_at": "...",
 *   "lifecycle": { "type": "buildpack|docker|cnb", "data": { "buildpacks": [...], "stack": "..." } },
 *   "metadata": { "labels": {...}, "annotations": {...} },
 *   "relationships": { "space": { "data": { "guid": "..." } } } }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record V3Application(@JsonProperty(V3Fields.GUID) String guid, @JsonProperty(V3Fields.NAME) String name,
                            @JsonProperty(V3Fields.STATE) String state,
                            @JsonProperty(V3Fields.CREATED_AT) String createdAt, @JsonProperty(V3Fields.UPDATED_AT) String updatedAt,
                            @JsonProperty("lifecycle") V3Lifecycle lifecycle, @JsonProperty(V3Fields.METADATA) V3Metadata metadata,
                            @JsonProperty(V3Fields.RELATIONSHIPS) V3Relationships relationships) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Lifecycle(@JsonProperty(V3Fields.TYPE) String type, @JsonProperty(V3Fields.DATA) V3LifecycleData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3LifecycleData(@JsonProperty("buildpacks") List<String> buildpacks, @JsonProperty("stack") String stack) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Relationships(@JsonProperty(V3Fields.SPACE) V3ToOneRelationship space) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3ToOneRelationship(@JsonProperty(V3Fields.DATA) V3RelationshipData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3RelationshipData(@JsonProperty(V3Fields.GUID) String guid) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3EnvironmentVariables(@JsonProperty("var") Map<String, String> environmentVariables) {
    }

}
