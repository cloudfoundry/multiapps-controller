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
 *
 * The {@code relationships.organization.data} discriminates shared domains (data == null) from private domains (data != null); this
 * mirrors the filtering the OSS impl applied on {@code DomainResource.getRelationships().getOrganization().getData()}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record V3Domain(@JsonProperty("guid") String guid, @JsonProperty("name") String name,
                       @JsonProperty("created_at") String createdAt, @JsonProperty("updated_at") String updatedAt,
                       @JsonProperty("metadata") V3Metadata metadata, @JsonProperty("relationships") V3DomainRelationships relationships) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3DomainRelationships(@JsonProperty("organization") V3ToOneRelationship organization) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3ToOneRelationship(@JsonProperty("data") V3RelationshipData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3RelationshipData(@JsonProperty("guid") String guid) {
    }

    /**
     * True when this domain is scoped to an organization (a private domain), i.e. its {@code relationships.organization.data} is present.
     * A shared domain has {@code data == null}. Mirrors the OSS {@code getPrivateDomainResources}/{@code getSharedDomainResources} filters.
     */
    public boolean isPrivate() {
        return relationships != null && relationships.organization() != null && relationships.organization()
                                                                                             .data() != null;
    }

}
