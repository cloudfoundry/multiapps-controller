package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Thin Jackson wire-model of a CF v3 app feature resource ({@code GET /v3/apps/{guid}/features}). App features belong to the
 * <em>application</em> entity.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record V3AppFeature(@JsonProperty(V3Fields.NAME) String name, @JsonProperty(V3Fields.ENABLED) Boolean enabled) {
}
