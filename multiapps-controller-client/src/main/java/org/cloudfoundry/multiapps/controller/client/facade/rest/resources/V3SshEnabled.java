package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Thin Jackson wire-model of the CF v3 SSH-enabled resource ({@code GET /v3/apps/{guid}/ssh_enabled}). This belongs to the
 * <em>application</em> entity.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record V3SshEnabled(@JsonProperty(V3Fields.ENABLED) Boolean enabled, @JsonProperty(V3Fields.REASON) String reason) {
}
