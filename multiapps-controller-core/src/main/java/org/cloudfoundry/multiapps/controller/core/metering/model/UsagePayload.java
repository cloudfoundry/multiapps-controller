package org.cloudfoundry.multiapps.controller.core.metering.model;

import java.util.Date;
import java.util.UUID;

import org.immutables.value.Value;

@Value.Immutable
public abstract class UsagePayload {

    @Value.Default
    public UUID id() {
        return UUID.randomUUID();
    }

    @Value.Default
    public Date timestamp() {
        return null;
    }

}
