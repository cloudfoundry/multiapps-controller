package org.cloudfoundry.multiapps.controller.persistence.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableApplicationShutdown.class)
@JsonDeserialize(as = ImmutableApplicationShutdown.class)
public interface ApplicationShutdown {

    enum Status {
        FINISHED, RUNNING, INITIAL
    }

    String getId();

    String getApplicationId();

    int getApplicationInstanceIndex();

    LocalDateTime getStartedAt();

    @Value.Default
    default Status getStatus() {
        return Status.INITIAL;
    }

}
