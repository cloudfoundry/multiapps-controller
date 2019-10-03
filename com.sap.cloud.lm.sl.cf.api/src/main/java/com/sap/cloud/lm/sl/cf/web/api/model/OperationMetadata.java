package com.sap.cloud.lm.sl.cf.web.api.model;

import java.util.List;
import java.util.Set;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloud.lm.sl.cf.web.api.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableOperationMetadata.class)
@JsonDeserialize(as = ImmutableOperationMetadata.class)
public interface OperationMetadata {

    Set<ParameterMetadata> getParameters();

    @Nullable
    String getDiagramId();

    /**
     * Can be used to ensure backwards compatibility when trying to find processes started with an older version of the application.
     */
    List<String> getPreviousDiagramIds();

    List<String> getVersions();

}
