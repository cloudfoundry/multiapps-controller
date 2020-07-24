package org.cloudfoundry.multiapps.controller.api.model;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

import org.cloudfoundry.multiapps.common.AllowNulls;
import org.cloudfoundry.multiapps.common.Nullable;
import org.cloudfoundry.multiapps.mta.model.AuditableConfiguration;
import org.cloudfoundry.multiapps.mta.model.ConfigurationIdentifier;
import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModelProperty;

@Value.Immutable
@JsonSerialize(as = ImmutableOperation.class)
@JsonDeserialize(as = ImmutableOperation.class)
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

        public static List<State> getNonFinalStates() {
            return Arrays.asList(RUNNING, ERROR, ACTION_REQUIRED);
        }

        public static List<State> getFinalStates() {
            return Arrays.asList(FINISHED, ABORTED);
        }

    }

    @Nullable
    public abstract String getProcessId();

    @Nullable
    @JsonSerialize(using = ProcessTypeSerializer.class)
    @JsonDeserialize(using = ProcessTypeDeserializer.class)
    public abstract ProcessType getProcessType();

    @Nullable
    @JsonSerialize(using = ZonedDateTimeSerializer.class)
    @JsonDeserialize(using = ZonedDateTimeDeserializer.class)
    public abstract ZonedDateTime getStartedAt();

    @Nullable
    @JsonSerialize(using = ZonedDateTimeSerializer.class)
    @JsonDeserialize(using = ZonedDateTimeDeserializer.class)
    public abstract ZonedDateTime getEndedAt();

    @Nullable
    public abstract String getSpaceId();

    @Nullable
    public abstract String getMtaId();

    @Nullable
    public abstract String getNamespace();

    @Nullable
    public abstract String getUser();

    @Nullable
    @JsonProperty("acquiredLock")
    public abstract Boolean hasAcquiredLock();

    @Nullable
    public abstract State getState();

    @Nullable
    public abstract ErrorType getErrorType();

    public abstract List<Message> getMessages();

    @AllowNulls
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
