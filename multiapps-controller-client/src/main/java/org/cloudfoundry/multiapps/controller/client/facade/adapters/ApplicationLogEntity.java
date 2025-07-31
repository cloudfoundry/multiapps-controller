package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.Map;

@Value.Immutable
@Value.Enclosing
@JsonDeserialize(as = ImmutableApplicationLogEntity.class)
public abstract class ApplicationLogEntity implements Comparable<ApplicationLogEntity> {

    @JsonProperty("timestamp")
    public abstract Long getTimestampInNanoseconds();

    @JsonProperty("source_id")
    public abstract String getSourceId();

    @JsonProperty("instance_id")
    public abstract String getInstanceId();

    public abstract Map<String, String> getTags();

    @JsonProperty("log")
    public abstract LogBody getLogBody();

    @Value.Immutable
    @JsonDeserialize(as = ImmutableApplicationLogEntity.ImmutableLogBody.class)
    public interface LogBody {

        @JsonProperty("payload")
        String getMessage();

        @JsonProperty("type")
        String getMessageType();
    }

    @Override
    public int compareTo(ApplicationLogEntity otherLog) {
        return getTimestampInNanoseconds().compareTo(otherLog.getTimestampInNanoseconds());
    }
}
