package com.sap.cloud.lm.sl.cf.core.health.model;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import javax.annotation.Nullable;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessTypeDeserializer;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessTypeSerializer;
import com.sap.cloud.lm.sl.cf.web.api.model.ZonedDateTimeDeserializer;
import com.sap.cloud.lm.sl.cf.web.api.model.ZonedDateTimeSerializer;

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
