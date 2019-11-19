package com.sap.cloud.lm.sl.cf.core.health.model;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessTypeDeserializer;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessTypeSerializer;
import com.sap.cloud.lm.sl.cf.web.api.model.ZonedDateTimeDeserializer;
import com.sap.cloud.lm.sl.cf.web.api.model.ZonedDateTimeSerializer;

public class HealthCheckOperation {

    private final String id;
    @JsonSerialize(using = ProcessTypeSerializer.class)
    @JsonDeserialize(using = ProcessTypeDeserializer.class)
    private final ProcessType type;
    @JsonSerialize(using = ZonedDateTimeSerializer.class)
    @JsonDeserialize(using = ZonedDateTimeDeserializer.class)
    private final ZonedDateTime startedAt;
    @JsonSerialize(using = ZonedDateTimeSerializer.class)
    @JsonDeserialize(using = ZonedDateTimeDeserializer.class)
    private final ZonedDateTime endedAt;
    private final long durationInSeconds;
    private final Operation.State state;
    private final String spaceId;
    private final String mtaId;
    private final String user;

    protected HealthCheckOperation(Builder builder) {
        this.id = builder.id;
        this.type = builder.type;
        this.startedAt = builder.startedAt;
        this.endedAt = builder.endedAt;
        this.durationInSeconds = builder.durationInSeconds;
        this.state = builder.state;
        this.spaceId = builder.spaceId;
        this.mtaId = builder.mtaId;
        this.user = builder.user;
    }

    public String getId() {
        return id;
    }

    public ProcessType getType() {
        return type;
    }

    public ZonedDateTime getStartedAt() {
        return startedAt;
    }

    public ZonedDateTime getEndedAt() {
        return endedAt;
    }

    public long getDurationInSeconds() {
        return durationInSeconds;
    }

    public Operation.State getState() {
        return state;
    }

    public String getSpaceId() {
        return spaceId;
    }

    public String getMtaId() {
        return mtaId;
    }

    public String getUser() {
        return user;
    }

    public static HealthCheckOperation fromOperation(Operation operation) {
        long durationInSeconds = ChronoUnit.SECONDS.between(operation.getStartedAt(), operation.getEndedAt());
        return new Builder().id(operation.getProcessId())
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

    public static class Builder {

        private String id;
        private ProcessType type;
        private ZonedDateTime startedAt;
        private ZonedDateTime endedAt;
        private long durationInSeconds;
        private Operation.State state;
        private String spaceId;
        private String mtaId;
        private String user;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder type(ProcessType type) {
            this.type = type;
            return this;
        }

        public Builder startedAt(ZonedDateTime startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public Builder endedAt(ZonedDateTime endedAt) {
            this.endedAt = endedAt;
            return this;
        }

        public Builder durationInSeconds(long durationInSeconds) {
            this.durationInSeconds = durationInSeconds;
            return this;
        }

        public Builder state(Operation.State state) {
            this.state = state;
            return this;
        }

        public Builder spaceId(String spaceId) {
            this.spaceId = spaceId;
            return this;
        }

        public Builder mtaId(String mtaId) {
            this.mtaId = mtaId;
            return this;
        }

        public Builder user(String user) {
            this.user = user;
            return this;
        }

        public HealthCheckOperation build() {
            return new HealthCheckOperation(this);
        }

    }

}
