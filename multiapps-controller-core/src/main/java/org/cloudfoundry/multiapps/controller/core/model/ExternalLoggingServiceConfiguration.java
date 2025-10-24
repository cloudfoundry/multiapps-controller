package org.cloudfoundry.multiapps.controller.core.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableExternalLoggingServiceConfiguration.class)
@JsonDeserialize(as = ImmutableExternalLoggingServiceConfiguration.class)
public interface ExternalLoggingServiceConfiguration {

    String getServiceInstanceName();

    String getServiceKeyName();

    @Nullable
    String getTargetOrg();

    @Nullable
    String getTargetSpace();
}
