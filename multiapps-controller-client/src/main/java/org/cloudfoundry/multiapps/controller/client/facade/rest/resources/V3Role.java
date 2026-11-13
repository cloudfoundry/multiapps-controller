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
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record V3Role(@JsonProperty(V3Fields.GUID) String guid, @JsonProperty(V3Fields.CREATED_AT) String createdAt,
                     @JsonProperty(V3Fields.UPDATED_AT) String updatedAt, @JsonProperty(V3Fields.TYPE) String type,
                     @JsonProperty(V3Fields.RELATIONSHIPS) V3Relationships relationships) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Relationships(@JsonProperty(V3Fields.USER) V3ToOneRelationship user,
                                  @JsonProperty(V3Fields.SPACE) V3ToOneRelationship space,
                                  @JsonProperty(V3Fields.ORGANIZATION) V3ToOneRelationship organization) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3ToOneRelationship(@JsonProperty(V3Fields.DATA) V3RelationshipData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3RelationshipData(@JsonProperty(V3Fields.GUID) String guid) {
    }

}
