package com.sap.cloud.lm.sl.cf.web.api.model;

import java.util.List;
import java.util.Set;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloud.lm.sl.common.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableOperationMetadata.class)
@JsonDeserialize(as = ImmutableOperationMetadata.class)
public interface OperationMetadata {

    Set<ParameterMetadata> getParameters();

    @Nullable
    String getDiagramId();

    List<String> getVersions();

}
