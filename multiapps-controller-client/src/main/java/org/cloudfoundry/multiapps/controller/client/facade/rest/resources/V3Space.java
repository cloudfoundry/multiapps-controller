package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Thin Jackson wire-model of a CF v3 space resource ({@code GET /v3/spaces/{guid}} / {@code GET /v3/spaces?...}).
 *
 * <pre>
 * { "guid": "...", "name": "...", "created_at": "...", "updated_at": "...",
 *   "relationships": { "organization": { "data": { "guid": "..." } } } }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record V3Space(@JsonProperty("guid") String guid, @JsonProperty("name") String name, @JsonProperty("created_at") String createdAt,
                      @JsonProperty("updated_at") String updatedAt, @JsonProperty("relationships") V3SpaceRelationships relationships) {

    public String organizationGuid() {
        if (relationships == null || relationships.organization() == null || relationships.organization()
                                                                                          .data() == null) {
            return null;
        }

        return relationships.organization()
                            .data()
                            .guid();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3SpaceRelationships(@JsonProperty("organization") V3ToOne organization) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3ToOne(@JsonProperty("data") V3Data data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Data(@JsonProperty("guid") String guid) {
    }

}
