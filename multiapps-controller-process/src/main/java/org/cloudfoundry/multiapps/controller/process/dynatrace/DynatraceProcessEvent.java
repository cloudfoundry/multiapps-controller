package org.cloudfoundry.multiapps.controller.process.dynatrace;

import org.cloudfoundry.multiapps.controller.process.dynatrace.ImmutableDynatraceProcessEvent;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableDynatraceProcessEvent.class)
@JsonDeserialize(as = ImmutableDynatraceProcessEvent.class)
public abstract class DynatraceProcessEvent implements DyntraceProcessEntity {

    public abstract EventType getEventType();

    public enum EventType {

        STARTED, FINISHED, FAILED;

        public static EventType fromValue(String v) {
            for (EventType b : EventType.values()) {
                if (b.name()
                     .equals(v)) {
                    return b;
                }
            }
            return null;
        }
    }

}
