package org.cloudfoundry.multiapps.controller.api.model;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

import org.cloudfoundry.multiapps.common.AllowNulls;
import org.cloudfoundry.multiapps.common.Nullable;
import org.cloudfoundry.multiapps.mta.model.AuditableConfiguration;
import org.cloudfoundry.multiapps.mta.model.ConfigurationIdentifier;
import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModelProperty;

@Value.Immutable
@JsonSerialize(as = ImmutableOperation.class)
@JsonDeserialize(as = ImmutableOperation.class)
@JsonPropertyOrder({ "processId", "processType", "startedAt", "endedAt", "spaceId", "mtaId", "namespace", "user", "acquiredLock", "state", "errorType", "messages", "parameters" })
public abstract class Operation implements AuditableConfiguration {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME;

    @XmlType(name = "")
    @XmlEnum(State.class)
    public enum State {

        RUNNING, FINISHED, ERROR, ABORTED, ACTION_REQUIRED;

        public static State fromValue(String v) {
            for (State b : State.values()) {
                if (b.name()
                     .equals(v)) {
                    return b;
                }
            }
            return null;
        }

        public boolean isFinal() {
            return getFinalStates().contains(this);
        }

        public static List<State> getNonFinalStates() {
            return Arrays.asList(RUNNING, ERROR, ACTION_REQUIRED);
        }

        public static List<State> getFinalStates() {
            return Arrays.asList(FINISHED, ABORTED);
        }

    }

    @Nullable
    @ApiModelProperty(accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public abstract String getProcessId();

    @Nullable
    @ApiModelProperty(accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @JsonSerialize(using = ProcessTypeSerializer.class)
    @JsonDeserialize(using = ProcessTypeDeserializer.class)
    public abstract ProcessType getProcessType();

    @Nullable
    @ApiModelProperty(accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @JsonSerialize(using = ZonedDateTimeSerializer.class)
    @JsonDeserialize(using = ZonedDateTimeDeserializer.class)
    public abstract ZonedDateTime getStartedAt();

    @Nullable
    @ApiModelProperty(accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @JsonSerialize(using = ZonedDateTimeSerializer.class)
    @JsonDeserialize(using = ZonedDateTimeDeserializer.class)
    public abstract ZonedDateTime getEndedAt();

    @Nullable
    @ApiModelProperty(accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public abstract String getSpaceId();

    @Nullable
    @ApiModelProperty(accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public abstract String getMtaId();

    @Nullable
    @ApiModelProperty(accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public abstract String getNamespace();

    @Nullable
    @ApiModelProperty(accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public abstract String getUser();

    @Nullable
    @ApiModelProperty(accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @JsonProperty("acquiredLock")
    public abstract Boolean hasAcquiredLock();

    @Nullable
    @ApiModelProperty(accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public abstract State getState();

    @Nullable
    @ApiModelProperty(accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public abstract ErrorType getErrorType();

    @ApiModelProperty(accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public abstract List<Message> getMessages();

    @AllowNulls
    @ApiModelProperty(accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public abstract Map<String, Object> getParameters();

    @Override
    @ApiModelProperty(hidden = true)
    public String getConfigurationType() {
        return "MTA operation";
    }

    @Override
    @ApiModelProperty(hidden = true)
    public String getConfigurationName() {
        return getProcessId();
    }

    @Override
    @ApiModelProperty(hidden = true)
    public List<ConfigurationIdentifier> getConfigurationIdentifiers() {
        List<ConfigurationIdentifier> identifiersList = new ArrayList<>();
        identifiersList.add(new ConfigurationIdentifier("process type", Objects.toString(getProcessType())));
        identifiersList.add(new ConfigurationIdentifier("started at", Objects.toString(getStartedAt())));
        identifiersList.add(new ConfigurationIdentifier("ended at", Objects.toString(getEndedAt())));
        identifiersList.add(new ConfigurationIdentifier("space id", getSpaceId()));
        identifiersList.add(new ConfigurationIdentifier("mta id", getMtaId()));
        identifiersList.add(new ConfigurationIdentifier("user", getUser()));
        identifiersList.add(new ConfigurationIdentifier("state", Objects.toString(getState())));
        identifiersList.add(new ConfigurationIdentifier("error type", Objects.toString(getErrorType())));
        return identifiersList;
    }

}
