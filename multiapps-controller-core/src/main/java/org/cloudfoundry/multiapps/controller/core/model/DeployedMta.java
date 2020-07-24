package org.cloudfoundry.multiapps.controller.core.model;

import java.util.List;

import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadata;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableDeployedMta.class)
@JsonDeserialize(builder = ImmutableDeployedMta.Builder.class)
public interface DeployedMta {

    MtaMetadata getMetadata();

    @Value.Auxiliary
    List<DeployedMtaApplication> getApplications();

    @Value.Auxiliary
    List<DeployedMtaService> getServices();

}
