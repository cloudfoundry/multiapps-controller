package org.cloudfoundry.multiapps.controller.core.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.multiapps.controller.client.facade.Nullable;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.immutables.value.Value;

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
