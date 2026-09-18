package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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
