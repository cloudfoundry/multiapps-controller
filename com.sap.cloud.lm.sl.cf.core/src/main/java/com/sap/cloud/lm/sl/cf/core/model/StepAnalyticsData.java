package com.sap.cloud.lm.sl.cf.core.model;

import org.immutables.value.Value;
import org.immutables.value.Value.Immutable;

@Immutable
public interface StepAnalyticsData {

    @Value.Default
    default long getId() {
        return 0;
    }

    String getProcessId();

    String getTaskId();

    StepEvent getEvent();

    @Value.Default
    default long getEventOccurrenceTime() {
        return System.currentTimeMillis();
    }

    enum StepEvent {
        STARTED, ENDED
    }
}
