package com.sap.cloud.lm.sl.cf.core.model;

import java.sql.Timestamp;
import java.util.Date;

import org.immutables.value.Value;
import org.immutables.value.Value.Immutable;

@Immutable
public interface HistoricOperationEvent {

    @Value.Default
    default long getId() {
        return 0;
    }

    String getProcessId();

    EventType getType();

    @Value.Default
    default Date getTimestamp() {
        return new Timestamp(System.currentTimeMillis());
    }

    public enum EventType {
        STARTED, FINISHED, FAILED_BY_CONTENT_ERROR, FAILED_BY_INFRASTRUCTURE_ERROR, RETRIED, ABORTED
    }

}