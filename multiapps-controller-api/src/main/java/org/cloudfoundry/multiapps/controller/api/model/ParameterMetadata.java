package org.cloudfoundry.multiapps.controller.api.model;

import org.cloudfoundry.multiapps.common.Nullable;
import org.cloudfoundry.multiapps.controller.api.model.parameters.ParameterConverter;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableParameterMetadata.class)
@JsonDeserialize(as = ImmutableParameterMetadata.class)
public interface ParameterMetadata {

    String getId();

    @Nullable
    Object getDefaultValue();

    @Value.Default
    default boolean getRequired() {
        return false;
    }

    ParameterType getType();

    @Nullable
    ParameterConverter getCustomConverter();

}
