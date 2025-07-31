package org.cloudfoundry.multiapps.controller.client.facade.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.multiapps.controller.client.facade.Nullable;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudEvent.ImmutableParticipant;

@Value.Enclosing
@Value.Immutable
@JsonSerialize(as = ImmutableCloudEvent.class)
@JsonDeserialize(as = ImmutableCloudEvent.class)
public abstract class CloudEvent extends CloudEntity implements Derivable<CloudEvent> {

    @Nullable
    public abstract String getType();

    @Nullable
    public abstract Participant getActor();

    @Nullable
    public abstract Participant getTarget();

    @Nullable
    public LocalDateTime getTimestamp() {
        return getMetadata().getCreatedAt();
    }

    @Override
    public CloudEvent derive() {
        return this;
    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableParticipant.class)
    @JsonDeserialize(as = ImmutableParticipant.class)
    public interface Participant {

        @Nullable
        UUID getGuid();

        @Nullable
        String getName();

        @Nullable
        String getType();

    }

}
