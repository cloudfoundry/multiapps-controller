package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Thin Jackson wire-model of a CF v3 space resource ({@code GET /v3/spaces/{guid}} / {@code GET /v3/spaces?...}).
 *
 * <pre>
 * { "guid": "...", "name": "...", "created_at": "...", "updated_at": "...",
 *   "relationships": { "organization": { "data": { "guid": "..." } }, "quota": { "data": { "guid": "..." } } },
 *   "suspended": false,
 *   "metadata": { "labels": {...}, "annotations": {...} }
 *   "links": { "self": { "href": "..." }, "scale": { "href": "..." }, ... },
 *   }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record V3Space(@JsonProperty(V3Fields.GUID) String guid, @JsonProperty(V3Fields.NAME) String name,
                      @JsonProperty(V3Fields.CREATED_AT) String createdAt,
                      @JsonProperty(V3Fields.UPDATED_AT) String updatedAt,
                      @JsonProperty(V3Fields.SUSPENDED) Boolean suspended,
                      @JsonProperty(V3Fields.RELATIONSHIPS) V3SpaceRelationships relationships,
                      @JsonProperty(V3Fields.METADATA) V3Metadata metadata,
                      @JsonProperty(V3Fields.LINKS) Map<String, V3Link> links) {

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
    public record V3SpaceRelationships(@JsonProperty(V3Fields.ORGANIZATION) V3ToOne organization,
                                       @JsonProperty(V3Fields.QUOTA) V3ToOne quota) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3ToOne(@JsonProperty(V3Fields.DATA) V3Data data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Data(@JsonProperty(V3Fields.GUID) String guid) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Link(@JsonProperty(V3Fields.HREF) String href) {
    }

}
