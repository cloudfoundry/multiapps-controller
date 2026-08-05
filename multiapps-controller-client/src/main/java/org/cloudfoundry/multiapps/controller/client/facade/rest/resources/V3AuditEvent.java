package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Thin Jackson wire-model of a CF v3 audit event resource ({@code GET /v3/audit_events}).
 *
 * <pre>
 * { "guid": "...", "created_at": "...", "updated_at": "...", "type": "audit.app.update",
 *   "actor":  { "guid": "...", "type": "user",        "name": "..." },
 *   "target": { "guid": "...", "type": "app",         "name": "..." },
 *   "data": { ... }, "space": {...}, "organization": {...} }
 * </pre>
 *
 * Only the fields the domain {@code CloudEvent} needs are mapped; everything else is ignored so CF can evolve the payload without breaking
 * us. Mirrors the CF v3 JSON consumed by the OSS {@code AuditEventResource} adapter (whose {@code actor}/{@code target} carry
 * {@code guid}/{@code name}/{@code type}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record V3AuditEvent(@JsonProperty("guid") String guid, @JsonProperty("created_at") String createdAt,
                           @JsonProperty("updated_at") String updatedAt, @JsonProperty("type") String type,
                           @JsonProperty("actor") V3Participant actor, @JsonProperty("target") V3Participant target) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Participant(@JsonProperty("guid") String guid, @JsonProperty("type") String type,
                                @JsonProperty("name") String name) {
    }

}
