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
    String getId();

    @Nullable
    String getTargetOrg();

    @Nullable
    String getTargetSpace();

    @Nullable
    String getMtaOrg();

    @Nullable
    String getMtaSpace();

    @Nullable
    String getMtaSpaceId();

    @Nullable
    String getMtaId();

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

    @Nullable
    String getServiceInstanceName();

    @Nullable
    String getServiceKeyName();
}
