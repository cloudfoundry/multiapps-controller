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
 *   "droplet": { "guid": "..." } }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record V3Build(@JsonProperty(V3Fields.GUID) String guid, @JsonProperty(V3Fields.CREATED_AT) String createdAt,
                      @JsonProperty(V3Fields.UPDATED_AT) String updatedAt, @JsonProperty(V3Fields.STATE) String state,
                      @JsonProperty("error") String error, @JsonProperty("created_by") V3CreatedBy createdBy,
                      @JsonProperty("package") V3PackageReference inputPackage, @JsonProperty("droplet") V3DropletReference droplet) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3CreatedBy(@JsonProperty(V3Fields.GUID) String guid, @JsonProperty(V3Fields.NAME) String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3PackageReference(@JsonProperty(V3Fields.GUID) String guid) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3DropletReference(@JsonProperty(V3Fields.GUID) String guid) {
    }

}
