package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Thin Jackson wire-models for the CF v3 <em>process</em> family of endpoints consumed by the client:
 *
 * <pre>
 * // process
 * { "guid": "...", "command": "...", "instances": 1, "memory_in_mb": 256, "disk_in_mb": 1024,
 *   "health_check": { "type": "port|http|process", "data": { "timeout": N, "invocation_timeout": N, "endpoint": "...", "interval": N } },
 *   "readiness_health_check": { "type": "process|http|port", "data": { "invocation_timeout": N, "endpoint": "...", "interval": N } } }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record V3Process(@JsonProperty("guid") String guid, @JsonProperty("command") String command,
                        @JsonProperty("instances") Integer instances, @JsonProperty("memory_in_mb") Integer memoryInMb,
                        @JsonProperty("disk_in_mb") Integer diskInMb, @JsonProperty("health_check") V3HealthCheck healthCheck,
                        @JsonProperty("readiness_health_check") V3HealthCheck readinessHealthCheck) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3HealthCheck(@JsonProperty("type") String type, @JsonProperty("data") V3HealthCheckData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3HealthCheckData(@JsonProperty("timeout") Integer timeout,
                                    @JsonProperty("invocation_timeout") Integer invocationTimeout,
                                    @JsonProperty("endpoint") String endpoint, @JsonProperty("interval") Integer interval) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3ProcessStats(@JsonProperty("resources") List<V3ProcessStatsResource> resources) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3ProcessStatsResource(@JsonProperty("index") Integer index, @JsonProperty("state") String state,
                                         @JsonProperty("routable") String routable) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3AppFeature(@JsonProperty("name") String name, @JsonProperty("enabled") Boolean enabled) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3SshEnabled(@JsonProperty("enabled") Boolean enabled, @JsonProperty("reason") String reason) {
    }

}
