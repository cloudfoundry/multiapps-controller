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
public record V3Stack(@JsonProperty("guid") String guid, @JsonProperty("name") String name,
                      @JsonProperty("description") String description, @JsonProperty("created_at") String createdAt,
                      @JsonProperty("updated_at") String updatedAt, @JsonProperty("metadata") V3Metadata metadata) {

}
