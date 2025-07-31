package org.cloudfoundry.multiapps.controller.client.facade.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.multiapps.controller.client.facade.Nullable;
import org.cloudfoundry.AllowNulls;
import org.immutables.value.Value;

import java.util.Map;

@Value.Immutable
@JsonSerialize(as = ImmutableLifecycle.class)
@JsonDeserialize(as = ImmutableLifecycle.class)
public interface Lifecycle {

    LifecycleType getType();

    @Nullable
    @AllowNulls
    Map<String, Object> getData();

}
