package org.cloudfoundry.multiapps.controller.client.lib.domain;

import java.util.Collections;
import java.util.Map;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableCfUserMetadata.class)
@JsonDeserialize(as = ImmutableCfUserMetadata.class)
public interface CfUserMetadata {

    @Value.Default
    default Map<String, String> getLabels() {
        return Collections.emptyMap();
    }

    @Value.Default
    default Map<String, String> getAnnotations() {
        return Collections.emptyMap();
    }

}
