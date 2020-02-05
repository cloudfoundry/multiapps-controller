package com.sap.cloud.lm.sl.cf.core.model;

import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Value.Immutable
@JsonSerialize(as = ImmutableDeployedMtaService.class)
@JsonDeserialize(builder = ImmutableDeployedMtaService.Builder.class)
public interface DeployedMtaService extends CloudService {

    @Nullable
    String getResourceName();

}
