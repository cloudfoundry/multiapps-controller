package org.cloudfoundry.multiapps.controller.client.facade.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.multiapps.controller.client.facade.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudProcess.class)
@JsonDeserialize(as = ImmutableCloudProcess.class)
public abstract class CloudProcess extends CloudEntity implements Derivable<CloudProcess> {

    public abstract String getCommand();

    public abstract Integer getDiskInMb();

    public abstract Integer getInstances();

    public abstract Integer getMemoryInMb();

    public abstract HealthCheckType getHealthCheckType();

    @Nullable
    public abstract String getHealthCheckHttpEndpoint();

    @Nullable
    public abstract Integer getHealthCheckInvocationTimeout();

    @Nullable
    public abstract Integer getHealthCheckTimeout();

    @Nullable
    public abstract Integer getReadinessHealthCheckInterval();

    @Nullable
    public abstract Integer getReadinessHealthCheckInvocationTimeout();

    @Nullable
    public abstract String getReadinessHealthCheckType();

    @Nullable
    public abstract String getReadinessHealthCheckHttpEndpoint();

    @Override
    public CloudProcess derive() {
        return this;
    }
}
