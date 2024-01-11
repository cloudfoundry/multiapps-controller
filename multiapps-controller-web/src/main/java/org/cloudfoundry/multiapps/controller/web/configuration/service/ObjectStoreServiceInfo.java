package org.cloudfoundry.multiapps.controller.web.configuration.service;

import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;
import org.jclouds.domain.Credentials;

import com.google.common.base.Supplier;

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

}
