package com.sap.cloud.lm.sl.cf.web.api.model;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloud.lm.sl.common.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableParameterMetadata.class)
@JsonDeserialize(as = ImmutableParameterMetadata.class)
public interface ParameterMetadata {

    @Nullable
    String getId();

    @Nullable
    Object getDefaultValue();

    @Nullable
    @Value.Default
    default Boolean getRequired() {
        return false;
    }

    @Nullable
    @Value.Default
    default ParameterType getType() {
        return ParameterType.STRING;
    }

    enum ParameterType {
        STRING, INTEGER, BOOLEAN, TABLE
    }

}
