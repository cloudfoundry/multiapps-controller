package com.sap.cloud.lm.sl.cf.web.api.model;

import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloud.lm.sl.cf.web.api.model.parameters.ParameterConverter;

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
