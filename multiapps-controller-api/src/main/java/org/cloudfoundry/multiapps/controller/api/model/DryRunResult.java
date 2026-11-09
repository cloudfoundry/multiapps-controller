package org.cloudfoundry.multiapps.controller.api.model;

import java.util.List;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableDryRunResult.class)
@JsonDeserialize(as = ImmutableDryRunResult.class)
public interface DryRunResult {

    @JsonProperty("appsToCreate")
    List<String> getAppsToCreate();

    @JsonProperty("appsToUpdate")
    List<String> getAppsToUpdate();

    @JsonProperty("appsToDelete")
    List<String> getAppsToDelete();

    @JsonProperty("servicesToCreate")
    List<String> getServicesToCreate();

    @JsonProperty("servicesToUpdate")
    List<String> getServicesToUpdate();

    @JsonProperty("servicesToDelete")
    List<String> getServicesToDelete();

    @JsonProperty("routesToCreate")
    List<String> getRoutesToCreate();

    @JsonProperty("routesToDelete")
    List<String> getRoutesToDelete();

}
