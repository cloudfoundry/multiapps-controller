package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Thin Jackson wire-model of a CF v3 route resource ({@code GET/POST /v3/routes}, {@code GET /v3/apps/{guid}/routes}).
 *
 * <pre>
 * { "guid": "...", "host": "...", "path": "...", "port": 8080, "url": "host.domain/path",
 *   "created_at": "...", "updated_at": "...",
 *   "destinations": [ { "guid": "...", "app": { "guid": "..." }, "port": 8080, "weight": 1, "protocol": "http1" } ],
 *   "metadata": { "labels": {...}, "annotations": {...} },
 *   "relationships": { "space": { "data": { "guid": "..." } }, "domain": { "data": { "guid": "..." } } } }
 * </pre>
 *
 * Mirrors exactly the fields the OSS {@code RawCloudRoute} adapter consumes from {@code org.cloudfoundry.client.v3.routes.Route}, so
 * both client implementations yield identical {@code CloudRoute} domain objects.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record V3Route(@JsonProperty("guid") String guid, @JsonProperty("host") String host, @JsonProperty("path") String path,
                      @JsonProperty("port") Integer port, @JsonProperty("url") String url,
                      @JsonProperty("created_at") String createdAt, @JsonProperty("updated_at") String updatedAt,
                      @JsonProperty("destinations") List<V3Destination> destinations, @JsonProperty("metadata") V3Metadata metadata,
                      @JsonProperty("relationships") V3RouteRelationships relationships) {

    public List<V3Destination> destinations() {
        return destinations == null ? Collections.emptyList() : destinations;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Destination(@JsonProperty("guid") String guid, @JsonProperty("app") V3DestinationApp app,
                                @JsonProperty("port") Integer port, @JsonProperty("weight") Integer weight,
                                @JsonProperty("protocol") String protocol) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3DestinationApp(@JsonProperty("guid") String guid) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3RouteRelationships(@JsonProperty("space") V3ToOneRelationship space,
                                       @JsonProperty("domain") V3ToOneRelationship domain) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3ToOneRelationship(@JsonProperty("data") V3RelationshipData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3RelationshipData(@JsonProperty("guid") String guid) {
    }

}
