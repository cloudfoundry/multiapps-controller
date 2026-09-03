package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Thin Jackson wire-model of a Cloud Controller v3 job resource ({@code GET /v3/jobs/{guid}}).
 *
 * <pre>
 * { "guid": "...", "created_at": "...", "updated_at": "...", "state": "PROCESSING|POLLING|COMPLETE|FAILED", "operation": "...",
 *   "errors": [ { "detail": "...", "title": "...", "code": N } ], "warnings": [ { "detail": "..." } ] }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record V3Job(@JsonProperty("guid") String guid, @JsonProperty("created_at") String createdAt,
                    @JsonProperty("updated_at") String updatedAt, @JsonProperty("state") String state,
                    @JsonProperty("operation") String operation, @JsonProperty("errors") List<V3Error> errors,
                    @JsonProperty("warnings") List<V3Warning> warnings) {

    public boolean isComplete() {
        return "COMPLETE".equals(state);
    }

    public boolean isFailed() {
        return "FAILED".equals(state);
    }

    public boolean isTerminal() {
        return isComplete() || isFailed();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Error(@JsonProperty("detail") String detail, @JsonProperty("title") String title, @JsonProperty("code") Integer code) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Warning(@JsonProperty("detail") String detail) {
    }

}
