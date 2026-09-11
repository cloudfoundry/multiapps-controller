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
public record V3ListResponse<R>(@JsonProperty("pagination") Pagination pagination, @JsonProperty("resources") List<R> resources) {

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
    public record Pagination(@JsonProperty("total_results") Integer totalResults, @JsonProperty("total_pages") Integer totalPages,
                             @JsonProperty("next") Link next, @JsonProperty("previous") Link previous) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Link(@JsonProperty("href") String href) {
    }

}
