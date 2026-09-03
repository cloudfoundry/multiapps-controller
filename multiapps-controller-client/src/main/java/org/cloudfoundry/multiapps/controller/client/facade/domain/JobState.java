package org.cloudfoundry.multiapps.controller.client.facade.domain;

import java.text.MessageFormat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.cloudfoundry.multiapps.controller.Messages;

public enum JobState {

    PROCESSING("processing"), POLLING("polling"), COMPLETE("complete"), FAILED("failed");

    private final String value;

    JobState(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static JobState from(String value) {
        for (JobState state : values()) {
            if (state.value.equals(value)) {
                return state;
            }
        }

        throw new IllegalArgumentException(MessageFormat.format(Messages.UNKNOWN_JOB_STATE_0, value));
    }
}
