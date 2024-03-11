package com.sap.cloud.lm.sl.cf.core.health.model;

public class HealthCheckConfiguration {

    private final String spaceId;
    private final String mtaId;
    private final long timeRangeInSeconds;
    private final String userName;

    private HealthCheckConfiguration(Builder builder) {
        this.spaceId = builder.spaceId;
        this.mtaId = builder.mtaId;
        this.timeRangeInSeconds = builder.timeRangeInSeconds;
        this.userName = builder.userName;
    }

    public String getSpaceId() {
        return spaceId;
    }

    public String getMtaId() {
        return mtaId;
    }

    public long getTimeRangeInSeconds() {
        return timeRangeInSeconds;
    }

    public String getUserName() {
        return userName;
    }

    public static class Builder {

        private String spaceId;
        private String mtaId;
        private long timeRangeInSeconds;
        private String userName;

        public Builder spaceId(String spaceId) {
            this.spaceId = spaceId;
            return this;
        }

        public Builder mtaId(String mtaId) {
            this.mtaId = mtaId;
            return this;
        }

        public Builder timeRangeInSeconds(long timeRangeInSeconds) {
            this.timeRangeInSeconds = timeRangeInSeconds;
            return this;
        }

        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public HealthCheckConfiguration build() {
            return new HealthCheckConfiguration(this);
        }

    }

}
