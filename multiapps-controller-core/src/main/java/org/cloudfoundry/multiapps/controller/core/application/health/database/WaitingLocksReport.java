package org.cloudfoundry.multiapps.controller.core.application.health.database;

import org.immutables.value.Value;

@Value.Immutable
public interface WaitingLocksReport {

    boolean hasIncreaseWaitingLocks();
}
