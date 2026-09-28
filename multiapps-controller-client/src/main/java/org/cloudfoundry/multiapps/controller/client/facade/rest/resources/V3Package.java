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
 *   "relationships": { "app": { "data": { "guid": "..." } } } }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record V3Package(@JsonProperty(V3Fields.GUID) String guid, @JsonProperty(V3Fields.TYPE) String type, @JsonProperty(V3Fields.STATE) String state,
                        @JsonProperty(V3Fields.CREATED_AT) String createdAt, @JsonProperty(V3Fields.UPDATED_AT) String updatedAt,
                        @JsonProperty(V3Fields.DATA) V3PackageData data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3PackageData(@JsonProperty("checksum") V3Checksum checksum, @JsonProperty("error") String error,
                                @JsonProperty("image") String image, @JsonProperty("username") String username,
                                @JsonProperty("password") String password) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Checksum(@JsonProperty(V3Fields.TYPE) String type, @JsonProperty("value") String value) {
    }

}
