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
public record V3Package(@JsonProperty("guid") String guid, @JsonProperty("type") String type, @JsonProperty("state") String state,
                        @JsonProperty("created_at") String createdAt, @JsonProperty("updated_at") String updatedAt,
                        @JsonProperty("data") V3PackageData data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3PackageData(@JsonProperty("checksum") V3Checksum checksum, @JsonProperty("error") String error,
                                @JsonProperty("image") String image, @JsonProperty("username") String username,
                                @JsonProperty("password") String password) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Checksum(@JsonProperty("type") String type, @JsonProperty("value") String value) {
    }

}
