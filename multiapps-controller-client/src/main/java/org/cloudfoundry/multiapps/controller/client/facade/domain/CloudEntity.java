package org.cloudfoundry.multiapps.controller.client.facade.domain;

import java.util.UUID;

import org.cloudfoundry.client.v3.Metadata;

import org.cloudfoundry.multiapps.controller.client.facade.Nullable;

/**
 * Do not extend {@code Derivable<T>} in this interface. It is tempting, because all of its children have the same implementation, but
 * implementing the {@code derive()} method here leads to this bug: https://github.com/immutables/immutables/issues/1045
 *
 */
public abstract class CloudEntity {

    @Nullable
    public abstract String getName();

    @Nullable
    public abstract CloudMetadata getMetadata();

    @Nullable
    public abstract Metadata getV3Metadata();

    public UUID getGuid() {
        return getMetadata().getGuid();
    }

}
