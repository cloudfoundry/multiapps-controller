package org.cloudfoundry.multiapps.controller.core.model;

import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableDynamicResolvableParameter.class)
@JsonDeserialize(as = ImmutableDynamicResolvableParameter.class)
public interface DynamicResolvableParameter {

    String getParameterName();

    @Nullable
    String getValue();

    String getRelationshipEntityName();

}
