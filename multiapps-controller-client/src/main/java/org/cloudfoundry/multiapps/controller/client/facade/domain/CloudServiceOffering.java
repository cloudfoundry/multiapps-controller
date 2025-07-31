package org.cloudfoundry.multiapps.controller.client.facade.domain;

import java.util.List;
import java.util.Map;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.multiapps.controller.client.facade.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudServiceOffering.class)
@JsonDeserialize(as = ImmutableCloudServiceOffering.class)
public abstract class CloudServiceOffering extends CloudEntity implements Derivable<CloudServiceOffering> {

    @Nullable
    public abstract Boolean isAvailable();

    @Nullable
    public abstract Boolean isBindable();

    @Nullable
    public abstract Boolean isShareable();

    public abstract List<CloudServicePlan> getServicePlans();

    @Nullable
    public abstract String getDescription();

    @Nullable
    public abstract String getDocUrl();

    @Nullable
    public abstract Map<String, Object> getExtra();

    @Nullable
    public abstract String getBrokerId();

    @Nullable
    public abstract String getUniqueId();

    @Override
    public CloudServiceOffering derive() {
        return this;
    }

}
