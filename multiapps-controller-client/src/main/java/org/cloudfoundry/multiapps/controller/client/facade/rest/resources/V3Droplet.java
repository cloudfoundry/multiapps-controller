package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Thin Jackson wire-model of the CF v3 current-droplet response ({@code GET /v3/apps/{guid}/droplets/current}):
 *
 * <pre>
 * { "guid": "...", "links": { "package": { "href": "https://.../v3/packages/{guid}" }, ... } }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record V3Droplet(@JsonProperty("guid") String guid, @JsonProperty("links") Map<String, V3Link> links) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Link(@JsonProperty("href") String href) {
    }

}
