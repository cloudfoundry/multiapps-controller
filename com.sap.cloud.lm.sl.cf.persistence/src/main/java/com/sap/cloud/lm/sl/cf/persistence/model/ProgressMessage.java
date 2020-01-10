package com.sap.cloud.lm.sl.cf.persistence.model;

import java.sql.Timestamp;
import java.util.Date;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import org.immutables.value.Value.Immutable;

@Immutable
@JsonSerialize(as = ImmutableProgressMessage.class)
@JsonDeserialize(as = ImmutableProgressMessage.class)
public interface ProgressMessage {

    @Value.Default
    default long getId() {
        return 0;
    }

    String getProcessId();

    String getTaskId();

    ProgressMessageType getType();

    String getText();

    @Value.Default
    default Date getTimestamp() {
        return new Timestamp(System.currentTimeMillis());
    }

    enum ProgressMessageType {
        ERROR, WARNING, INFO, EXT, TASK_STARTUP,
    }
}
