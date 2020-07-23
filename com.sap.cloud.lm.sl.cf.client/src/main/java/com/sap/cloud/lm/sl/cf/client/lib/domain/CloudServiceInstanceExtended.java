package com.sap.cloud.lm.sl.cf.client.lib.domain;

import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudServiceInstanceExtended.class)
@JsonDeserialize(as = ImmutableCloudServiceInstanceExtended.class)
public abstract class CloudServiceInstanceExtended extends CloudServiceInstance {

    public abstract List<String> getAlternativeLabels();

    @Nullable
    public abstract String getResourceName();

    @Value.Default
    public boolean isOptional() {
        return false;
    }

    @Value.Default
    public boolean isManaged() {
        return false;
    }

    @Value.Default
    public boolean shouldIgnoreUpdateErrors() {
        return false;
    }

}
