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
public record V3Application(@JsonProperty("guid") String guid, @JsonProperty("name") String name, @JsonProperty("state") String state,
                            @JsonProperty("created_at") String createdAt, @JsonProperty("updated_at") String updatedAt,
                            @JsonProperty("lifecycle") V3Lifecycle lifecycle, @JsonProperty("metadata") V3Metadata metadata,
                            @JsonProperty("relationships") V3Relationships relationships) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Lifecycle(@JsonProperty("type") String type, @JsonProperty("data") V3LifecycleData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3LifecycleData(@JsonProperty("buildpacks") List<String> buildpacks, @JsonProperty("stack") String stack) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Relationships(@JsonProperty("space") V3ToOneRelationship space) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3ToOneRelationship(@JsonProperty("data") V3RelationshipData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3RelationshipData(@JsonProperty("guid") String guid) {
    }

    /**
     * The CF v3 app environment variables response ({@code GET /v3/apps/{guid}/environment_variables}):
     * {@code { "var": { "KEY": "VALUE", ... } }}.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3EnvironmentVariables(@JsonProperty("var") Map<String, String> var) {
    }

}
