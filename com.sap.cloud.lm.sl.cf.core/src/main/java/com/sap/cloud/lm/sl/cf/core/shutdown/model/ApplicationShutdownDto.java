package com.sap.cloud.lm.sl.cf.core.shutdown.model;

public class ApplicationShutdownDto {
    private Status status = Status.RUNNING;
    private String appId;
    private String appInstanceId;
    private String appInstanceIndex;
    private long cooldownTimeoutInSeconds;

    private ApplicationShutdownDto(Status status, String appId,
        String appInstanceId, String appInstanceIndex, long cooldownTimeoutInSeconds) {
        this.status = status;
        this.appId = appId;
        this.appInstanceId = appInstanceId;
        this.appInstanceIndex = appInstanceIndex;
        this.cooldownTimeoutInSeconds = cooldownTimeoutInSeconds;
    }

    public Status getStatus() {
        return status;
    }

    public String getAppId() {
        return appId;
    }

    public String getAppInstanceId() {
        return appInstanceId;
    }

    public String getAppInstanceIndex() {
        return appInstanceIndex;
    }

    public long getCooldownTimeoutInSeconds() {
        return cooldownTimeoutInSeconds;
    }

    public static class Builder {
        private boolean isActive = true;
        private String appId;
        private String appInstanceId;
        private String appInstanceIndex;
        private long cooldownTimeoutInSeconds;

        public ApplicationShutdownDto build(){
            return new ApplicationShutdownDto(isActive ? Status.RUNNING : Status.FINISHED, appId, appInstanceId,
                appInstanceIndex,
                cooldownTimeoutInSeconds);
        }

        public Builder isActive(boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public Builder appId(String appId) {
            this.appId = appId;
            return this;
        }

        public Builder appInstanceId(String appInstanceId) {
            this.appInstanceId = appInstanceId;
            return this;
        }

        public Builder appInstanceIndex(String appInstanceIndex) {
            this.appInstanceIndex = appInstanceIndex;
            return this;
        }

        public Builder cooldownTimeoutInSeconds(long cooldownTimeoutInSeconds) {
            this.cooldownTimeoutInSeconds = cooldownTimeoutInSeconds;
            return this;
        }
    }

    public enum Status {
        FINISHED, RUNNING;
    }
}
