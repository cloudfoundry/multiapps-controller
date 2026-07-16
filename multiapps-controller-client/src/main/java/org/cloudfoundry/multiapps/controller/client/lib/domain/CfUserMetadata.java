package org.cloudfoundry.multiapps.controller.client.lib.domain;

import java.util.Collections;
import java.util.Map;

import org.immutables.value.Value;

@Value.Immutable
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
