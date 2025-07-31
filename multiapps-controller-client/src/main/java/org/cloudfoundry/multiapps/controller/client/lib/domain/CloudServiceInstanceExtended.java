package org.cloudfoundry.multiapps.controller.client.lib.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.multiapps.common.Nullable;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudServiceInstanceExtended.class)
@JsonDeserialize(as = ImmutableCloudServiceInstanceExtended.class)
public abstract class CloudServiceInstanceExtended extends CloudServiceInstance {

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
    public boolean shouldSkipParametersUpdate() {
        return false;
    }

    @Value.Default
    public boolean shouldSkipPlanUpdate() {
        return false;
    }

    @Value.Default
    public boolean shouldSkipTagsUpdate() {
        return false;
    }

    @Value.Default
    public boolean shouldSkipSyslogUrlUpdate() {
        return false;
    }

    @Nullable
    public abstract Boolean shouldFailOnParametersUpdateFailure();

    @Nullable
    public abstract Boolean shouldFailOnPlanUpdateFailure();

    @Nullable
    public abstract Boolean shouldFailOnTagsUpdateFailure();

}
