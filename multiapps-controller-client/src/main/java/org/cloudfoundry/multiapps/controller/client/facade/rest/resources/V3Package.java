package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Thin Jackson wire-model of a CF v3 package resource ({@code GET/POST /v3/packages}, {@code GET /v3/apps/{guid}/packages}).
 *
 * <pre>
 * { "guid": "...", "type": "bits|docker", "state": "AWAITING_UPLOAD|PROCESSING_UPLOAD|READY|FAILED|COPYING|EXPIRED",
 *   "created_at": "...", "updated_at": "...",
 *   "data": {
 *     // bits:
 *     "checksum": { "type": "sha256", "value": "..." }, "error": "...",
 *     // docker:
 *     "image": "...", "username": "...", "password": "..."
 *   },
 *   "relationships": { "app": { "data": { "guid": "..." } } }
 *   "metadata": { "labels": {...}, "annotations": {...} }}
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record V3Package(@JsonProperty(V3Fields.GUID) String guid, @JsonProperty(V3Fields.TYPE) String type,
                        @JsonProperty(V3Fields.STATE) String state,
                        @JsonProperty(V3Fields.CREATED_AT) String createdAt, @JsonProperty(V3Fields.UPDATED_AT) String updatedAt,
                        @JsonProperty(V3Fields.DATA) V3PackageData data, @JsonProperty(V3Fields.METADATA) V3Metadata metadata,
                        @JsonProperty(V3Fields.RELATIONSHIPS) V3Relationships relationships) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3PackageData(@JsonProperty(V3Fields.CHECKSUM) V3Checksum checksum, @JsonProperty(V3Fields.ERROR) String error,
                                @JsonProperty(V3Fields.IMAGE) String image, @JsonProperty(V3Fields.USERNAME) String username,
                                @JsonProperty(V3Fields.PASSWORD) String password) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Checksum(@JsonProperty(V3Fields.TYPE) String type, @JsonProperty(V3Fields.VALUE) String value) {
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
