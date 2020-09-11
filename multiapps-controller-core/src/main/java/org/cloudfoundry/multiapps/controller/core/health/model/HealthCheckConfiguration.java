package org.cloudfoundry.multiapps.controller.core.health.model;

import org.cloudfoundry.multiapps.common.Nullable;
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
