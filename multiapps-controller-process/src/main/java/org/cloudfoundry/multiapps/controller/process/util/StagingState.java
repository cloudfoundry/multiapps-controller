package org.cloudfoundry.multiapps.controller.process.util;

import org.cloudfoundry.multiapps.common.Nullable;
import org.cloudfoundry.multiapps.controller.client.facade.domain.PackageState;
import org.immutables.value.Value;

@Value.Immutable
public interface StagingState {

    PackageState getState();

    @Nullable
    String getError();

}
