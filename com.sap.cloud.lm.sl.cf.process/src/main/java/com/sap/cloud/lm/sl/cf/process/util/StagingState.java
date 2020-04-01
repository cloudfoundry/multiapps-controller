package com.sap.cloud.lm.sl.cf.process.util;

import org.cloudfoundry.client.lib.domain.PackageState;
import org.immutables.value.Value;

import com.sap.cloud.lm.sl.common.Nullable;

@Value.Immutable
public interface StagingState {

    PackageState getState();

    @Nullable
    String getError();

}
