package org.cloudfoundry.multiapps.controller.client.facade.domain;

import java.util.Map;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.multiapps.controller.client.facade.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudServicePlan.class)
@JsonDeserialize(as = ImmutableCloudServicePlan.class)
public abstract class CloudServicePlan extends CloudEntity implements Derivable<CloudServicePlan> {

    @Nullable
    public abstract String getDescription();

    @Nullable
    public abstract Map<String, Object> getExtra();

    @Nullable
    public abstract String getUniqueId();

    @Nullable
    public abstract String getServiceOfferingId();

    @Nullable
    public abstract Boolean isFree();

    @Nullable
    public abstract Boolean isPublic();

    @Override
    public CloudServicePlan derive() {
        return this;
    }

}
