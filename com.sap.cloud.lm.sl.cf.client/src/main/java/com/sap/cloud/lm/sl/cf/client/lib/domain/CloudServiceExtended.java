package com.sap.cloud.lm.sl.cf.client.lib.domain;

import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloud.lm.sl.common.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudServiceExtended.class)
@JsonDeserialize(as = ImmutableCloudServiceExtended.class)
public interface CloudServiceExtended extends CloudService {

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
