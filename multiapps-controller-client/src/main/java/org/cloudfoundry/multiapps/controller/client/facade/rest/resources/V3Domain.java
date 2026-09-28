package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Thin Jackson wire-model of a CF v3 domain resource ({@code GET/POST /v3/domains}, {@code GET /v3/organizations/{guid}/domains},
 * {@code GET /v3/organizations/{guid}/domains/default}).
 *
 * <pre>
 * { "guid": "...", "name": "example.com", "created_at": "...", "updated_at": "...",
 *   "metadata": { "labels": {...}, "annotations": {...} },
 *   "relationships": { "organization": { "data": { "guid": "..." } | null } } }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record V3Domain(@JsonProperty(V3Fields.GUID) String guid, @JsonProperty(V3Fields.NAME) String name,
                       @JsonProperty(V3Fields.CREATED_AT) String createdAt, @JsonProperty(V3Fields.UPDATED_AT) String updatedAt,
                       @JsonProperty(V3Fields.METADATA) V3Metadata metadata,
                       @JsonProperty(V3Fields.RELATIONSHIPS) V3DomainRelationships relationships) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3DomainRelationships(@JsonProperty(V3Fields.ORGANIZATION) V3ToOneRelationship organization) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3ToOneRelationship(@JsonProperty(V3Fields.DATA) V3RelationshipData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3RelationshipData(@JsonProperty(V3Fields.GUID) String guid) {
    }

    public boolean isPrivate() {
        return relationships != null && relationships.organization() != null && relationships.organization()
                                                                                             .data() != null;
    }

}
