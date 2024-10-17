package org.cloudfoundry.multiapps.controller.core.model;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloudfoundry.client.facade.Nullable;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;

@Value.Immutable
@JsonSerialize(as = ImmutableIncrementalAppInstanceUpdateConfiguration.class)
@JsonDeserialize(as = ImmutableIncrementalAppInstanceUpdateConfiguration.class)
public interface IncrementalAppInstanceUpdateConfiguration {

    @Nullable
    CloudApplication getOldApplication();

    @Nullable
    Integer getOldApplicationInstanceCount();

    @Nullable
    Integer getOldApplicationInitialInstanceCount();

    CloudApplication getNewApplication();

    Integer getNewApplicationInstanceCount();
}
