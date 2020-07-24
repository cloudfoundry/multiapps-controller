package org.cloudfoundry.multiapps.controller.core.model;

import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableDeployedMtaApplication.class)
@JsonDeserialize(builder = ImmutableDeployedMtaApplication.Builder.class)
public abstract class DeployedMtaApplication extends CloudApplication {

    public abstract String getModuleName();

    public abstract List<String> getBoundMtaServices();

    public abstract List<String> getProvidedDependencyNames();

    @Value.Default
    public ProductizationState getProductizationState() {
        return ProductizationState.LIVE;
    }

    public enum ProductizationState {
        LIVE, IDLE
    }

}