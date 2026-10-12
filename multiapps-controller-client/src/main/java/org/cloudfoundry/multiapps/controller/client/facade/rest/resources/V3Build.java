package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Thin Jackson wire-model of a CF v3 build resource ({@code GET/POST /v3/builds}, {@code GET /v3/apps/{guid}/builds}).
 *
 * <pre>
 * { "guid": "...", "created_at": "...", "updated_at": "...",
 *   "state": "STAGING|STAGED|FAILED", "error": "...",
 *   "created_by": { "guid": "...", "name": "..." },
 *   "package": { "guid": "..." },
 *   "droplet": { "guid": "..." },
 *   "metadata": { "labels": {...}, "annotations": {...} },
 *   "relationships": { "app": { "data": { "guid": "..." } } } }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record V3Build(@JsonProperty(V3Fields.GUID) String guid, @JsonProperty(V3Fields.CREATED_AT) String createdAt,
                      @JsonProperty(V3Fields.UPDATED_AT) String updatedAt, @JsonProperty(V3Fields.STATE) String state,
                      @JsonProperty(V3Fields.ERROR) String error, @JsonProperty(V3Fields.CREATED_BY) V3CreatedBy createdBy,
                      @JsonProperty(V3Fields.PACKAGE) V3PackageReference inputPackage,
                      @JsonProperty(V3Fields.DROPLET) V3DropletReference droplet, @JsonProperty(V3Fields.METADATA) V3Metadata metadata,
                      @JsonProperty(V3Fields.RELATIONSHIPS) V3Relationships relationships) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3CreatedBy(@JsonProperty(V3Fields.GUID) String guid, @JsonProperty(V3Fields.NAME) String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3PackageReference(@JsonProperty(V3Fields.GUID) String guid) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3DropletReference(@JsonProperty(V3Fields.GUID) String guid) {
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

}
