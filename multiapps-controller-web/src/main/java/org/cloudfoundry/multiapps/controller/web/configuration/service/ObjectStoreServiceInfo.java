package org.cloudfoundry.multiapps.controller.web.configuration.service;

import java.util.Map;

import com.google.common.base.Supplier;
import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;
import org.jclouds.domain.Credentials;

@Value.Immutable
public interface ObjectStoreServiceInfo {

    String getProvider();

    @Nullable
    String getIdentity();

    @Nullable
    Supplier<Credentials> getCredentialsSupplier();

    @Nullable
    String getCredential();

    @Nullable
    String getContainer();

    @Nullable
    String getEndpoint();

    @Nullable
    String getRegion();

    @Nullable
    String getHost();

    @Nullable
    Map<String, Object> getCredentials();
}
