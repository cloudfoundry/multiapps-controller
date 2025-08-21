package org.cloudfoundry.multiapps.controller.client.facade.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.multiapps.controller.client.facade.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableInstanceInfo.class)
@JsonDeserialize(as = ImmutableInstanceInfo.class)
public interface InstanceInfo {

    int getIndex();

    InstanceState getState();

    @Nullable
    Boolean isRoutable();
}
