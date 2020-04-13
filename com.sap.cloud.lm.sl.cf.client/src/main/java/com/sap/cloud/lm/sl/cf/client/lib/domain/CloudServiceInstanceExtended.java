package com.sap.cloud.lm.sl.cf.client.lib.domain;

import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloud.lm.sl.common.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudServiceInstanceExtended.class)
@JsonDeserialize(as = ImmutableCloudServiceInstanceExtended.class)
public interface CloudServiceInstanceExtended extends CloudServiceInstance {

    List<String> getAlternativeLabels();

    @Nullable
    String getResourceName();

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
