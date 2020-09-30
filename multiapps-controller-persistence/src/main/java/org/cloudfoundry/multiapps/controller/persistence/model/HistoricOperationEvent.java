package org.cloudfoundry.multiapps.controller.persistence.model;

import java.util.Date;

import org.immutables.value.Value;

@Value.Immutable
public interface HistoricOperationEvent {

    @Value.Default
    default long getId() {
        return 0;
    }

    @Value.Parameter
    String getProcessId();

    @Value.Parameter
    EventType getType();

    @Value.Default
    @Value.Auxiliary // Makes sure timestamps won't be taken into account when comparing events in unit tests.
    default Date getTimestamp() {
        return new Date();
    }

    enum EventType {
        STARTED, FINISHED, FAILED_BY_CONTENT_ERROR, FAILED_BY_INFRASTRUCTURE_ERROR, RETRIED, ABORTED, ABORT_EXECUTED
    }

}