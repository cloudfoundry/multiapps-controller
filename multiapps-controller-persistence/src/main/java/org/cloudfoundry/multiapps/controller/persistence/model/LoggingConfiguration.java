package org.cloudfoundry.multiapps.controller.persistence.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableLoggingConfiguration.class)
@JsonDeserialize(as = ImmutableLoggingConfiguration.class)
public interface LoggingConfiguration {

    @Nullable
    String getTargetOrg();

    @Nullable
    String getTargetSpace();

    @Nullable
    String getOperationId();

    @Nullable
    String getEndpointUrl();

    @Nullable
    String getServerCa();

    @Nullable
    String getClientCert();

    @Nullable
    String getClientKey();

    @Nullable
    LogLevel getLogLevel();

    @Nullable
    Boolean isFailSafe();
}
