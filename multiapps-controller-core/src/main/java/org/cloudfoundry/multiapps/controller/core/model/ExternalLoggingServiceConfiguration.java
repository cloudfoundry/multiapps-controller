package org.cloudfoundry.multiapps.controller.core.model;

import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableExternalLoggingServiceConfiguration.class)
@JsonDeserialize(as = ImmutableExternalLoggingServiceConfiguration.class)
public interface ExternalLoggingServiceConfiguration {

    @Nullable
    String getServiceInstanceName();

    @Nullable
    String getServiceKeyName();

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
    List<String> getLogLevels();

    @Nullable
    Boolean isFailSafe();
}
