package org.cloudfoundry.multiapps.controller.core.application.health.model;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloudfoundry.client.facade.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableApplicationHealthResult.class)
@JsonDeserialize(as = ImmutableApplicationHealthResult.class)
public interface ApplicationHealthResult {

    Status getStatus();

    @Nullable
    Long countOfProcessesWaitingForLocks();

    Boolean hasIncreasedLocks();

    enum Status {
        UP, DOWN
    }

}
