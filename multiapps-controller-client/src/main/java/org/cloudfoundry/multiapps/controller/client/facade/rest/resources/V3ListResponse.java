package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Thin Jackson wire-model of a Cloud Controller v3 <em>list</em> response envelope:
 *
 * <pre>
 * {
 *   "pagination": { "total_results": N, "next": { "href": "..." } | null, ... },
 *   "resources":  [ ... ]
 * }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record V3ListResponse<R>(@JsonProperty(V3Fields.PAGINATION) Pagination pagination,
                                @JsonProperty(V3Fields.RESOURCES) List<R> resources) {

    public List<R> resources() {
        return resources == null ? Collections.emptyList() : resources;
    }

    public String nextPageHref() {
        if (pagination == null || pagination.next() == null) {
            return null;
        }

        return pagination.next()
                         .href();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pagination(@JsonProperty(V3Fields.TOTAL_RESULTS) Integer totalResults,
                             @JsonProperty(V3Fields.TOTAL_PAGES) Integer totalPages,
                             @JsonProperty(V3Fields.NEXT) Link next, @JsonProperty(V3Fields.PREVIOUS) Link previous) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Link(@JsonProperty(V3Fields.HREF) String href) {
    }

}
