package org.cloudfoundry.multiapps.controller.client.facade.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * State of an asynchronous Cloud Controller v3 job. Project-owned replacement for {@code org.cloudfoundry.client.v3.jobs.JobState},
 * so the domain model no longer depends on the OSS cf-java-client. JSON (de)serialization uses the lowercase wire value
 * ({@code processing} / {@code polling} / {@code complete} / {@code failed}) via {@link JsonValue} / {@link JsonCreator} — matching the
 * OSS enum so persisted models round-trip identically.
 */
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
        throw new IllegalArgumentException("Unknown job state: " + value);
    }
}
