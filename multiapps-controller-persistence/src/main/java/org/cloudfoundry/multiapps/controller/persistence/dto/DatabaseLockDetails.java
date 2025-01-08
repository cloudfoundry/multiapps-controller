package org.cloudfoundry.multiapps.controller.persistence.dto;

import org.immutables.value.Value;

@Value.Immutable
public interface DatabaseLockDetails {

    int getPid();

    String getApplicationName();

    String getLockType();

    String getMode();

    boolean isLockGranted();

}
