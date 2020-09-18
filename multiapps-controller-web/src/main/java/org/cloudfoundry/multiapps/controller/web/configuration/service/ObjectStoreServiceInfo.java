package org.cloudfoundry.multiapps.controller.web.configuration.service;

import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;

@Value.Immutable
public interface ObjectStoreServiceInfo {

    String getProvider();

    String getIdentity();

    String getCredential();

    String getContainer();

    @Nullable
    String getEndpoint();

    @Nullable
    String getRegion();

}
