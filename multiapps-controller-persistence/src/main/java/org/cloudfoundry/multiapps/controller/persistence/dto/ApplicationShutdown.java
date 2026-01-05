package org.cloudfoundry.multiapps.controller.persistence.dto;

import java.util.Date;

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

    Date getStaredAt();

    @Value.Default
    default String getStatus() {
        return Status.INITIAL.name();
    }

}
