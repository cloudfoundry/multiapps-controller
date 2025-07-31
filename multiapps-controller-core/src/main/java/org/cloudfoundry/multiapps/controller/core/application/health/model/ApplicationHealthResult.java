package org.cloudfoundry.multiapps.controller.core.application.health.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.multiapps.controller.client.facade.Nullable;
import org.immutables.value.Value;

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
