package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Thin Jackson wire-model of a CF v3 role resource ({@code GET /v3/roles}).
 *
 * <pre>
 * { "guid": "...", "created_at": "...", "updated_at": "...",
 *   "type": "organization_auditor|organization_billing_manager|organization_manager|organization_user|space_auditor|space_developer|space_manager",
 *   "relationships": { "user": { "data": { "guid": "..." } }, "space": { "data": { "guid": "..." } | null },
 *                      "organization": { "data": { "guid": "..." } | null } } }
 * </pre>
 *
 * Only {@code type} is consumed by the mapper (mirroring the OSS {@code RawUserRole}); the remaining fields are documented for
 * completeness and ignored so CF can evolve the payload without breaking us.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record V3Role(@JsonProperty("guid") String guid, @JsonProperty("created_at") String createdAt,
                     @JsonProperty("updated_at") String updatedAt, @JsonProperty("type") String type) {
}
