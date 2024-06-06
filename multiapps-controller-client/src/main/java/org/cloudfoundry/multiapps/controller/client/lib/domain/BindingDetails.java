package org.cloudfoundry.multiapps.controller.client.lib.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;

import java.util.Map;

@Value.Immutable
@JsonSerialize(as = ImmutableBindingDetails.class)
@JsonDeserialize(as = ImmutableBindingDetails.class)
public interface BindingDetails {

    @Nullable
    String getBindingName();

    @Nullable
    Map<String, Object> getConfig();
}