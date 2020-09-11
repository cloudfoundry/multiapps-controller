package org.cloudfoundry.multiapps.controller.core.health.model;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.cloudfoundry.multiapps.common.Nullable;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.api.model.ProcessTypeDeserializer;
import org.cloudfoundry.multiapps.controller.api.model.ProcessTypeSerializer;
import org.cloudfoundry.multiapps.controller.api.model.ZonedDateTimeDeserializer;
import org.cloudfoundry.multiapps.controller.api.model.ZonedDateTimeSerializer;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableHealthCheckOperation.class)
@JsonDeserialize(as = ImmutableHealthCheckOperation.class)
public interface HealthCheckOperation {

    @Nullable
    String getId();

    @Nullable
    @JsonSerialize(using = ProcessTypeSerializer.class)
    @JsonDeserialize(using = ProcessTypeDeserializer.class)
    ProcessType getType();

    @Nullable
    @JsonSerialize(using = ZonedDateTimeSerializer.class)
    @JsonDeserialize(using = ZonedDateTimeDeserializer.class)
    ZonedDateTime getStartedAt();

    @Nullable
    @JsonSerialize(using = ZonedDateTimeSerializer.class)
    @JsonDeserialize(using = ZonedDateTimeDeserializer.class)
    ZonedDateTime getEndedAt();

    @Value.Default
    default long getDurationInSeconds() {
        return 0;
    }

    @Nullable
    Operation.State getState();

    @Nullable
    String getSpaceId();

    @Nullable
    String getMtaId();

    @Nullable
    String getUser();

    static HealthCheckOperation fromOperation(Operation operation) {
        long durationInSeconds = ChronoUnit.SECONDS.between(operation.getStartedAt(), operation.getEndedAt());
        return ImmutableHealthCheckOperation.builder()
                                            .id(operation.getProcessId())
                                            .type(operation.getProcessType())
                                            .startedAt(operation.getStartedAt())
                                            .endedAt(operation.getEndedAt())
                                            .durationInSeconds(durationInSeconds)
                                            .state(operation.getState())
                                            .spaceId(operation.getSpaceId())
                                            .mtaId(operation.getMtaId())
                                            .user(operation.getUser())
                                            .build();
    }

}
