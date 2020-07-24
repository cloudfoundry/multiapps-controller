package org.cloudfoundry.multiapps.controller.process.util;

import org.cloudfoundry.client.lib.domain.PackageState;
import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;

@Value.Immutable
public interface StagingState {

    PackageState getState();

    @Nullable
    String getError();

}
