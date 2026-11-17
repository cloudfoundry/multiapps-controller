package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Thin Jackson wire-models for the CF v3 <em>process</em> family of endpoints consumed by the client:
 *
 * <pre>
 * // process
 * { "guid": "...", "type": "web", "command": "...", "user": "vcap", "instances": 1,
 *   "memory_in_mb": 256, "disk_in_mb": 1024, "log_rate_limit_in_bytes_per_second": 1024,
 *   "health_check": { "type": "port|http|process", "data": { "timeout": N, "invocation_timeout": N, "endpoint": "...", "interval": N } },
 *   "readiness_health_check": { "type": "process|http|port", "data": { "invocation_timeout": N, "endpoint": "...", "interval": N } },
 *   "relationships": { "app": { "data": { "guid": "..." } }, "revision": { "data": { "guid": "..." } } },
 *   "metadata": { "labels": {...}, "annotations": {...} },
 *   "created_at": "...", "updated_at": "...", "version": "...",
 *   "links": { "self": { "href": "..." }, "scale": { "href": "..." }, ... } }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record V3Process(@JsonProperty(V3Fields.GUID) String guid, @JsonProperty(V3Fields.COMMAND) String command,
                        @JsonProperty(V3Fields.INSTANCES) Integer instances, @JsonProperty(V3Fields.MEMORY_IN_MB) Integer memoryInMb,
                        @JsonProperty(V3Fields.DISK_IN_MB) Integer diskInMb, @JsonProperty(V3Fields.HEALTH_CHECK) V3HealthCheck healthCheck,
                        @JsonProperty(V3Fields.READINESS_HEALTH_CHECK) V3HealthCheck readinessHealthCheck,
                        @JsonProperty(V3Fields.TYPE) String type, @JsonProperty(V3Fields.USER) String user,
                        @JsonProperty(V3Fields.LOG_RATE_LIMIT_IN_BYTES_PER_SECOND) Integer logRateLimitInBytesPerSecond,
                        @JsonProperty(V3Fields.RELATIONSHIPS) V3Relationships relationships,
                        @JsonProperty(V3Fields.METADATA) V3Metadata metadata, @JsonProperty(V3Fields.CREATED_AT) String createdAt,
                        @JsonProperty(V3Fields.UPDATED_AT) String updatedAt, @JsonProperty(V3Fields.VERSION) String version,
                        @JsonProperty(V3Fields.LINKS) Map<String, V3Link> links) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3HealthCheck(@JsonProperty(V3Fields.TYPE) String type, @JsonProperty(V3Fields.DATA) V3HealthCheckData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3HealthCheckData(@JsonProperty(V3Fields.TIMEOUT) Integer timeout,
                                    @JsonProperty(V3Fields.INVOCATION_TIMEOUT) Integer invocationTimeout,
                                    @JsonProperty(V3Fields.ENDPOINT) String endpoint, @JsonProperty(V3Fields.INTERVAL) Integer interval) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Relationships(@JsonProperty(V3Fields.APP) V3ToOneRelationship app,
                                  @JsonProperty(V3Fields.REVISION) V3ToOneRelationship revision) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3ToOneRelationship(@JsonProperty(V3Fields.DATA) V3RelationshipData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3RelationshipData(@JsonProperty(V3Fields.GUID) String guid) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3Link(@JsonProperty(V3Fields.HREF) String href) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3ProcessStats(@JsonProperty(V3Fields.RESOURCES) List<V3ProcessStatsResource> resources) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3ProcessStatsResource(@JsonProperty(V3Fields.INDEX) Integer index, @JsonProperty(V3Fields.STATE) String state,
                                         @JsonProperty(V3Fields.ROUTABLE) String routable) {
    }

}
