package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Thin Jackson wire-model of a CF v3 organization resource ({@code GET /v3/organizations/{guid}} / {@code GET /v3/organizations?...}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record V3Organization(@JsonProperty("guid") String guid, @JsonProperty("name") String name,
                             @JsonProperty("created_at") String createdAt, @JsonProperty("updated_at") String updatedAt) {
}
