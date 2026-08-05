package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Thin Jackson wire-models for the CF v3 <em>process</em> family of endpoints consumed by the client:
 * <ul>
 * <li>{@link V3Process} — {@code GET /v3/apps/{guid}/processes/web} (also {@code GET /v3/processes/{guid}});</li>
 * <li>{@link V3ProcessStats} — {@code GET /v3/apps/{guid}/processes/web/stats};</li>
 * <li>{@link V3AppFeatures} / {@link V3AppFeature} — {@code GET /v3/apps/{guid}/features};</li>
 * <li>{@link V3SshEnabled} — {@code GET /v3/apps/{guid}/ssh_enabled}.</li>
 * </ul>
 *
 * <pre>
 * // process
 * { "guid": "...", "command": "...", "instances": 1, "memory_in_mb": 256, "disk_in_mb": 1024,
 *   "health_check": { "type": "port|http|process", "data": { "timeout": N, "invocation_timeout": N, "endpoint": "...", "interval": N } },
 *   "readiness_health_check": { "type": "process|http|port", "data": { "invocation_timeout": N, "endpoint": "...", "interval": N } } }
 * </pre>
 *
 * Every record ignores unknown fields so CF can evolve the payload without breaking us.
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

    /**
     * The CF v3 process statistics response ({@code GET /v3/apps/{guid}/processes/web/stats}):
     * {@code { "resources": [ { "index": 0, "state": "RUNNING", "routable": true }, ... ] }}.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3ProcessStats(@JsonProperty("resources") List<V3ProcessStatsResource> resources) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3ProcessStatsResource(@JsonProperty("index") Integer index, @JsonProperty("state") String state,
                                         @JsonProperty("routable") String routable) {
    }

    /**
     * The CF v3 app features response ({@code GET /v3/apps/{guid}/features}):
     * {@code { "resources": [ { "name": "ssh", "enabled": true }, ... ] }}.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3AppFeature(@JsonProperty("name") String name, @JsonProperty("enabled") Boolean enabled) {
    }

    /**
     * The CF v3 app SSH-enabled response ({@code GET /v3/apps/{guid}/ssh_enabled}):
     * {@code { "enabled": true, "reason": "..." }}.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record V3SshEnabled(@JsonProperty("enabled") Boolean enabled, @JsonProperty("reason") String reason) {
    }

}
