package org.cloudfoundry.multiapps.controller.web.configuration.service;

import java.util.Map;

import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;

@Value.Immutable
public interface ObjectStoreServiceInfo {

    String getProvider();

    @Nullable
    Map<String, Object> getCredentials();
}
