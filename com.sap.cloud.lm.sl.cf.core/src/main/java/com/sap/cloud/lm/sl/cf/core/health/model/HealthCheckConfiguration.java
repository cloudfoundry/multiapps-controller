package com.sap.cloud.lm.sl.cf.core.health.model;

import javax.annotation.Nullable;

import org.immutables.value.Value;

@Value.Immutable
public interface HealthCheckConfiguration {

    @Nullable
    String getSpaceId();

    @Nullable
    String getMtaId();

    @Value.Default
    default long getTimeRangeInSeconds() {
        return 0;
    }

    @Nullable
    String getUserName();

}
