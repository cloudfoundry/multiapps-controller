package org.cloudfoundry.multiapps.controller.api.model;

import java.util.List;
import java.util.Set;

import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;

import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableOperationMetadata.class)
@JsonDeserialize(as = ImmutableOperationMetadata.class)
public interface OperationMetadata {

    Set<ParameterMetadata> getParameters();

    @Nullable
    String getDiagramId();

    List<String> getVersions();

}
