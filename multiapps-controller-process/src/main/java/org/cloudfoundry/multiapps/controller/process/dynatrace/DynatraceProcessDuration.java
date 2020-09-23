package org.cloudfoundry.multiapps.controller.process.dynatrace;

import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableDynatraceProcessDuration.class)
@JsonDeserialize(as = ImmutableDynatraceProcessDuration.class)
public abstract class DynatraceProcessDuration implements DyntraceProcessEntity {

    public abstract long getProcessDuration();

    public abstract Operation.State getOperationState();
}
