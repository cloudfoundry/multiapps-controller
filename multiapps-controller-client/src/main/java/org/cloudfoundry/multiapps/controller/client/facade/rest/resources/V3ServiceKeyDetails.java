package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Thin Jackson wire-model of the CF v3 service credential binding <em>details</em> response
 * ({@code GET /v3/service_credential_bindings/{guid}/details}), which is where the actual {@code credentials} of a service key live.
 *
 * <pre>
 * { "credentials": { ... }, "syslog_drain_url": "...", "volume_mounts": [ ... ] }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record V3ServiceKeyDetails(@JsonProperty("credentials") Map<String, Object> credentials,
                                  @JsonProperty("syslog_drain_url") String syslogDrainUrl) {

    public Map<String, Object> credentials() {
        return credentials == null ? Collections.emptyMap() : credentials;
    }

}
