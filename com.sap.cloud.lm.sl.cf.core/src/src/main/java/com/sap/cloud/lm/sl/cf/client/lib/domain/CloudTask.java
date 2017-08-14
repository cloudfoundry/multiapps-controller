package com.sap.cloud.lm.sl.cf.client.lib.domain;

import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudEntity;

public class CloudTask extends CloudEntity {

    public enum State {
        PENDING, RUNNING, SUCCEEDED, CANCELING, FAILED;
    }

    public static class Result {

        private String failureReason;

        public Result() {
        }

        public Result(String failureReason) {
            this.failureReason = failureReason;
        }

        public String getFailureReason() {
            return failureReason;
        }

        public void setFailureReason(String failureReason) {
            this.failureReason = failureReason;
        }

    }

    private String command;
    private Map<String, String> environmentVariables;
    private Result result;
    private State state;

    public CloudTask(Meta meta, String name) {
        super(meta, name);
    }

    public CloudTask(Meta meta, String name, String command, Map<String, String> environmentVariables, State state, Result result) {
        super(meta, name);
        this.command = command;
        this.environmentVariables = environmentVariables;
        this.result = result;
        this.state = state;
    }

    public String getCommand() {
        return command;
    }

    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }

    public Result getResult() {
        return result;
    }

    public State getState() {
        return state;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public void setEnvironmentVariables(Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public void setState(State state) {
        this.state = state;
    }

}
