package org.cloudfoundry.multiapps.controller.process.event;

import org.cloudfoundry.multiapps.common.Nullable;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.api.model.ProcessTypeDeserializer;
import org.cloudfoundry.multiapps.controller.api.model.ProcessTypeSerializer;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableDynatraceProcessEvent.class)
@JsonDeserialize(as = ImmutableDynatraceProcessEvent.class)
public abstract class DynatraceProcessEvent {
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
    
    public abstract String getProcessId();

    @JsonSerialize(using = ProcessTypeSerializer.class)
    @JsonDeserialize(using = ProcessTypeDeserializer.class)
    public abstract ProcessType getProcessType();
    
    public abstract String getSpaceId();

    @Nullable
    public abstract String getMtaId();
    
    public abstract EventType getEventType();
}
