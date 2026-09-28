package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Thin Jackson wire-model of a CF v3 stack resource ({@code GET /v3/stacks}).
 *
 * <pre>
 * { "guid": "...", "name": "cflinuxfs4", "description": "...",
 *   "created_at": "...", "updated_at": "...",
 *   "metadata": { "labels": {...}, "annotations": {...} } }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record V3Stack(@JsonProperty(V3Fields.GUID) String guid, @JsonProperty(V3Fields.NAME) String name,
                      @JsonProperty(V3Fields.DESCRIPTION) String description, @JsonProperty(V3Fields.CREATED_AT) String createdAt,
                      @JsonProperty(V3Fields.UPDATED_AT) String updatedAt, @JsonProperty(V3Fields.METADATA) V3Metadata metadata) {

}
