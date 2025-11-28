package org.cloudfoundry.multiapps.controller.persistence.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableApplicationShutdown.class)
@JsonDeserialize(as = ImmutableApplicationShutdown.class)
public interface ApplicationShutdown {

    enum Status {
        FINISHED, RUNNING,
    }

    String getApplicationId();

    String getApplicationInstanceId();

    int getApplicationInstanceIndex();

    @Value.Default
    default Status getStatus() {
        return Status.RUNNING;
    }

}
