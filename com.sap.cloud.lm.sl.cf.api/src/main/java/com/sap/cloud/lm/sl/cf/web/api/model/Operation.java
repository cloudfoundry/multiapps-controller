package com.sap.cloud.lm.sl.cf.web.api.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.JsonAdapter;

import io.swagger.annotations.ApiModelProperty;

public class Operation {

    private String processId = null;
    @JsonAdapter(ProcessTypeJsonAdapter.class)
    private ProcessType processType = null;
    private String startedAt = null;
    private String spaceId = null;
    private String mtaId = null;
    private String user = null;
    private Boolean acquiredLock = null;
    private State state = null;
    private List<Message> messages = new ArrayList<Message>();
    private Map<String, Object> parameters = new HashMap<String, Object>();

    /**
     **/
    @ApiModelProperty(value = "")
    @JsonProperty("processId")
    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public Operation processId(String processId) {
        this.processId = processId;
        return this;
    }

    /**
     **/
    public Operation processType(ProcessType processType) {
        this.processType = processType;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("processType")
    public ProcessType getProcessType() {
        return processType;
    }

    public void setProcessType(ProcessType processType) {
        this.processType = processType;
    }

    /**
     **/
    public Operation startedAt(String startedAt) {
        this.startedAt = startedAt;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("startedAt")
    public String getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(String startedAt) {
        this.startedAt = startedAt;
    }

    /**
     **/
    public Operation spaceId(String spaceId) {
        this.spaceId = spaceId;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("spaceId")
    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(String spaceId) {
        this.spaceId = spaceId;
    }

    /**
     **/
    public Operation mtaId(String mtaId) {
        this.mtaId = mtaId;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("mtaId")
    public String getMtaId() {
        return mtaId;
    }

    public void setMtaId(String mtaId) {
        this.mtaId = mtaId;
    }

    /**
     **/
    public Operation user(String user) {
        this.user = user;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("user")
    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    /**
     **/
    public Operation acquiredLock(Boolean acquiredLock) {
        this.acquiredLock = acquiredLock;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("acquiredLock")
    public Boolean isAcquiredLock() {
        return acquiredLock;
    }

    public void setAcquiredLock(Boolean acquiredLock) {
        this.acquiredLock = acquiredLock;
    }

    /**
     **/
    public Operation state(State state) {
        this.state = state;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("state")
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    /**
     **/
    public Operation messages(List<Message> messages) {
        this.messages = messages;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("messages")
    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    /**
     **/
    public Operation parameters(Map<String, Object> parameters) {
        this.parameters = parameters;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("parameters")
    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Operation operation = (Operation) o;
        return Objects.equals(processId, operation.processId) && Objects.equals(processType, operation.processType)
            && Objects.equals(startedAt, operation.startedAt) && Objects.equals(spaceId, operation.spaceId)
            && Objects.equals(mtaId, operation.mtaId) && Objects.equals(user, operation.user)
            && Objects.equals(acquiredLock, operation.acquiredLock) && Objects.equals(state, operation.state)
            && Objects.equals(messages, operation.messages) && Objects.equals(parameters, operation.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processId, processType, startedAt, spaceId, mtaId, user, acquiredLock, state, messages, parameters);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Operation {\n");

        sb.append("    processId: ").append(toIndentedString(processId)).append("\n");
        sb.append("    processType: ").append(toIndentedString(processType)).append("\n");
        sb.append("    startedAt: ").append(toIndentedString(startedAt)).append("\n");
        sb.append("    spaceId: ").append(toIndentedString(spaceId)).append("\n");
        sb.append("    mtaId: ").append(toIndentedString(mtaId)).append("\n");
        sb.append("    user: ").append(toIndentedString(user)).append("\n");
        sb.append("    acquiredLock: ").append(toIndentedString(acquiredLock)).append("\n");
        sb.append("    state: ").append(toIndentedString(state)).append("\n");
        sb.append("    messages: ").append(toIndentedString(messages)).append("\n");
        sb.append("    parameters: ").append(toIndentedString(parameters)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces (except the first line).
     */
    private String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}