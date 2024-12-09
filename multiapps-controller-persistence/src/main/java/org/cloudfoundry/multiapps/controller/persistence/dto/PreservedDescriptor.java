package org.cloudfoundry.multiapps.controller.persistence.dto;

import java.time.LocalDateTime;

import org.cloudfoundry.multiapps.common.Nullable;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.immutables.value.Value;

@Value.Immutable
public interface PreservedDescriptor {

    @Value.Default
    default long getId() {
        return 0L;
    }

    DeploymentDescriptor getDescriptor();

    String getMtaId();

    String getMtaVersion();

    String getSpaceId();

    @Nullable
    String getNamespace();

    String getChecksum();

    @Value.Default
    default LocalDateTime getTimestamp() {
        return LocalDateTime.now();
    }

}
