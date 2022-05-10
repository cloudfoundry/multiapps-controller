package org.cloudfoundry.multiapps.controller.persistence.model;

import org.immutables.value.Value;

import java.time.LocalDateTime;

@Value.Immutable
public interface LockOwnerEntry {

    @Value.Default
    default long getId() {
        return 0;
    }

    String getLockOwner();

    LocalDateTime getTimestamp();
}
