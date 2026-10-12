package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LogCacheReadResponse(@JsonProperty(LogCacheFields.ENVELOPES) EnvelopeBatch envelopes) {

    public List<Envelope> batch() {
        if (envelopes == null || envelopes.batch() == null) {
            return Collections.emptyList();
        }

        return envelopes.batch();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EnvelopeBatch(@JsonProperty(LogCacheFields.BATCH) List<Envelope> batch) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Envelope(@JsonProperty(LogCacheFields.SOURCE_ID) String sourceId, @JsonProperty(LogCacheFields.TIMESTAMP) Long timestamp,
                           @JsonProperty(LogCacheFields.TAGS) Map<String, String> tags, @JsonProperty(LogCacheFields.LOG) Log log) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Log(@JsonProperty(LogCacheFields.PAYLOAD) String payload, @JsonProperty(LogCacheFields.TYPE) String type) {
    }

}
