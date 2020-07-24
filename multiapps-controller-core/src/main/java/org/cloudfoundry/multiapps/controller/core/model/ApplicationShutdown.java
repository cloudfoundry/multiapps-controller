package org.cloudfoundry.multiapps.controller.core.model;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableApplicationShutdown.class)
@JsonDeserialize(as = ImmutableApplicationShutdown.class)
public interface ApplicationShutdown {

    enum Status {
        FINISHED, RUNNING
    }

    String getApplicationId();

    String getApplicationInstanceId();

    int getApplicationInstanceIndex();

    @Value.Default
    default Status getStatus() {
        return Status.RUNNING;
    }

}
