package com.sap.cloud.lm.sl.cf.web.api.model;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.JsonAdapter;
import com.sap.cloud.lm.sl.mta.model.AuditableConfiguration;
import com.sap.cloud.lm.sl.mta.model.ConfigurationIdentifier;

import io.swagger.annotations.ApiModelProperty;

public class Operation implements AuditableConfiguration {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME;

    private String processId = null;
    @JsonAdapter(ProcessTypeJsonAdapter.class)
    private ProcessType processType = null;
    @JsonAdapter(ZonedDateTimeJsonAdapter.class)
    private ZonedDateTime startedAt = null;
    @JsonAdapter(ZonedDateTimeJsonAdapter.class)
    private ZonedDateTime endedAt = null;
    private String spaceId = null;
    private String mtaId = null;
    private String user = null;
    private Boolean acquiredLock = null;
    private Boolean cleanedUp = null;
    private State state = null;
    private List<Message> messages = new ArrayList<Message>();
    private Map<String, Object> parameters = new HashMap<String, Object>();

    /**
     **/
    public Operation processId(String processId) {
        this.processId = processId;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("processId")
    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
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
    public Operation startedAt(ZonedDateTime startedAt) {
        this.startedAt = startedAt;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("startedAt")
    public ZonedDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(ZonedDateTime startedAt) {
        this.startedAt = startedAt;
    }

    /**
     **/
    public Operation endedAt(ZonedDateTime endedAt) {
        this.endedAt = endedAt;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("endedAt")
    public ZonedDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(ZonedDateTime endedAt) {
        this.endedAt = endedAt;
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
    public Boolean hasAcquiredLock() {
        return acquiredLock;
    }

    public void setAcquiredLock(Boolean acquiredLock) {
        this.acquiredLock = acquiredLock;
    }

    /**
     **/
    public Operation cleanedUp(Boolean cleanedUp) {
        this.cleanedUp = cleanedUp;
        return this;
    }

    @ApiModelProperty(value = "")
    @JsonProperty("cleanedUp")
    public Boolean isCleanedUp() {
        return cleanedUp;
    }

    public void setCleanedUp(Boolean cleanedUp) {
        this.cleanedUp = cleanedUp;
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Operation operation = (Operation) o;
        return Objects.equals(processId, operation.processId) && Objects.equals(processType, operation.processType)
            && Objects.equals(startedAt, operation.startedAt) && Objects.equals(endedAt, operation.endedAt)
            && Objects.equals(spaceId, operation.spaceId) && Objects.equals(mtaId, operation.mtaId) && Objects.equals(user, operation.user)
            && Objects.equals(acquiredLock, operation.acquiredLock) && Objects.equals(cleanedUp, operation.cleanedUp)
            && Objects.equals(state, operation.state) && Objects.equals(messages, operation.messages)
            && Objects.equals(parameters, operation.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processId, processType, startedAt, endedAt, spaceId, mtaId, user, acquiredLock, cleanedUp, state, messages,
            parameters);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Operation {\n");

        sb.append("    processId: ").append(toIndentedString(processId)).append("\n");
        sb.append("    processType: ").append(toIndentedString(processType)).append("\n");
        sb.append("    startedAt: ").append(toIndentedString(startedAt)).append("\n");
        sb.append("    endedAt: ").append(toIndentedString(endedAt)).append("\n");
        sb.append("    spaceId: ").append(toIndentedString(spaceId)).append("\n");
        sb.append("    mtaId: ").append(toIndentedString(mtaId)).append("\n");
        sb.append("    user: ").append(toIndentedString(user)).append("\n");
        sb.append("    acquiredLock: ").append(toIndentedString(acquiredLock)).append("\n");
        sb.append("    cleanedUp: ").append(toIndentedString(cleanedUp)).append("\n");
        sb.append("    state: ").append(toIndentedString(state)).append("\n");
        sb.append("    messages: ").append(toIndentedString(messages)).append("\n");
        sb.append("    parameters: ").append(toIndentedString(parameters)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }

    @Override
    public String getConfigurationType() {
        return "MTA operation";
    }

    @Override
    public String getConfiguratioName() {
        return processId;
    }

    @Override
    public List<ConfigurationIdentifier> getConfigurationIdentifiers() {
        List<ConfigurationIdentifier> identifiersList = new ArrayList<>();
        identifiersList.add(new ConfigurationIdentifier("process type", Objects.toString(processType)));
        identifiersList.add(new ConfigurationIdentifier("started at", Objects.toString(startedAt)));
        identifiersList.add(new ConfigurationIdentifier("ended at", Objects.toString(endedAt)));
        identifiersList.add(new ConfigurationIdentifier("space id", spaceId));
        identifiersList.add(new ConfigurationIdentifier("mta id", mtaId));
        identifiersList.add(new ConfigurationIdentifier("user", user));
        identifiersList.add(new ConfigurationIdentifier("state", Objects.toString(state)));
        return identifiersList;
    }
}
