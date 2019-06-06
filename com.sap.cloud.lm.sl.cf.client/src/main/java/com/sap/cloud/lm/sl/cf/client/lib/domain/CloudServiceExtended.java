package com.sap.cloud.lm.sl.cf.client.lib.domain;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.Nullable;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudServiceExtended.class)
@JsonDeserialize(as = ImmutableCloudServiceExtended.class)
public interface CloudServiceExtended extends CloudService {

    List<String> getAlternativeLabels();

    @Nullable
    String getResourceName();

    Map<String, Object> getCredentials();

    List<String> getTags();

    @Value.Default
    default boolean isOptional() {
        return false;
    }

    @Value.Default
    default boolean isManaged() {
        return false;
    }

    @Value.Default
    default boolean shouldIgnoreUpdateErrors() {
        return false;
    }

}
