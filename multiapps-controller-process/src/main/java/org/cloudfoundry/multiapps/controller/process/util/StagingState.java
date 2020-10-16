package org.cloudfoundry.multiapps.controller.process.util;

import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;

import com.sap.cloudfoundry.client.facade.domain.PackageState;

@Value.Immutable
public interface StagingState {

    PackageState getState();

    @Nullable
    String getError();

}
