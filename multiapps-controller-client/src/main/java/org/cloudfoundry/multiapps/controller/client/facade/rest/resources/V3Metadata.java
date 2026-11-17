package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Thin Jackson wire-model of the CF v3 {@code metadata} object (labels + annotations) attached to most resources.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record V3Metadata(@JsonProperty(V3Fields.LABELS) Map<String, String> labels,
                         @JsonProperty(V3Fields.ANNOTATIONS) Map<String, String> annotations) {

    public Map<String, String> labels() {
        return labels == null ? Collections.emptyMap() : labels;
    }

    public Map<String, String> annotations() {
        return annotations == null ? Collections.emptyMap() : annotations;
    }

}
