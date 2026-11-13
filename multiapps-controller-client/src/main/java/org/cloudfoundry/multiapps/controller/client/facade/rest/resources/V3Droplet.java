package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Thin Jackson wire-model of a CF v3 droplet resource ({@code GET /v3/droplets/{guid}},
 * {@code GET /v3/apps/{guid}/droplets/current}).
 *
 * <pre>
 * { "guid": "...", "created_at": "...", "updated_at": "...",
 *   "state": "AWAITING_UPLOAD|PROCESSING_UPLOAD|STAGED|COPYING|FAILED|EXPIRED", "error": "...",
 *   "lifecycle": { "type": "buildpack|docker|cnb", "data": { "buildpacks": [...], "stack": "..." } },
 *   "execution_metadata": "...", "process_types": { "web": "..." },
 *   "relationships": { "app": { "data": { "guid": "..." } } },
 *   "metadata": { "labels": {...}, "annotations": {...} },
 *   "links": { "package": { "href": "https://.../v3/packages/{guid}" }, ... } }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record V3Droplet(@JsonProperty(V3Fields.GUID) String guid, @JsonProperty(V3Fields.CREATED_AT) String createdAt,
                        @JsonProperty(V3Fields.UPDATED_AT) String updatedAt, @JsonProperty(V3Fields.STATE) String state,
                        @JsonProperty(V3Fields.ERROR) String error, @JsonProperty(V3Fields.LIFECYCLE) V3Lifecycle lifecycle,
                        @JsonProperty(V3Fields.EXECUTION_METADATA) String executionMetadata,
                        @JsonProperty(V3Fields.PROCESS_TYPES) Map<String, String> processTypes,
                        @JsonProperty(V3Fields.RELATIONSHIPS) V3Relationships relationships,
                        @JsonProperty(V3Fields.METADATA) V3Metadata metadata, @JsonProperty(V3Fields.LINKS) Map<String, V3Link> links) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Lifecycle(@JsonProperty(V3Fields.TYPE) String type, @JsonProperty(V3Fields.DATA) V3LifecycleData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3LifecycleData(@JsonProperty(V3Fields.BUILDPACKS) List<String> buildpacks, @JsonProperty(V3Fields.STACK) String stack) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Relationships(@JsonProperty(V3Fields.APP) V3ToOneRelationship app) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3ToOneRelationship(@JsonProperty(V3Fields.DATA) V3RelationshipData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3RelationshipData(@JsonProperty(V3Fields.GUID) String guid) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Link(@JsonProperty(V3Fields.HREF) String href) {
    }

}
