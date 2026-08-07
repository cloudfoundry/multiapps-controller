package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Thin Jackson wire-model of a Log-Cache {@code read} response ({@code GET /api/v1/read/{source_id}}), which is a plain JSON/REST API:
 *
 * <pre>
 * { "envelopes": { "batch": [
 *     { "source_id": "...", "timestamp": "1699999999000000000", "tags": { "source_type": "APP/PROC/WEB" },
 *       "log": { "payload": "&lt;base64&gt;", "type": "OUT" | "ERR" } } ] } }
 * </pre>
 *
 * Replaces the OSS {@code org.cloudfoundry.logcache.v1.*} types so the log-cache client no longer depends on cf-java-client. Only the
 * fields the mapper needs are declared.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LogCacheReadResponse(@JsonProperty("envelopes") EnvelopeBatch envelopes) {

    public List<Envelope> batch() {
        if (envelopes == null || envelopes.batch() == null) {
            return Collections.emptyList();
        }
        return envelopes.batch();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EnvelopeBatch(@JsonProperty("batch") List<Envelope> batch) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Envelope(@JsonProperty("source_id") String sourceId, @JsonProperty("timestamp") Long timestamp,
                           @JsonProperty("tags") Map<String, String> tags, @JsonProperty("log") Log log) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Log(@JsonProperty("payload") String payload, @JsonProperty("type") String type) {
    }

}
