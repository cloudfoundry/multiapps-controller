package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Thin Jackson wire-model of a CF v3 organization resource ({@code GET /v3/organizations/{guid}} / {@code GET /v3/organizations?...}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record V3Organization(@JsonProperty(V3Fields.GUID) String guid, @JsonProperty(V3Fields.NAME) String name,
                             @JsonProperty(V3Fields.CREATED_AT) String createdAt, @JsonProperty(V3Fields.UPDATED_AT) String updatedAt) {
}
