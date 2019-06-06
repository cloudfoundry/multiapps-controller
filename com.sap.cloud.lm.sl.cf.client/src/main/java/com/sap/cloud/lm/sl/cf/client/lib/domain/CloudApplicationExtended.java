package com.sap.cloud.lm.sl.cf.client.lib.domain;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.Nullable;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.cloudfoundry.client.lib.domain.CloudTask;
import org.cloudfoundry.client.lib.domain.DockerInfo;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudApplicationExtended.ImmutableAttributeUpdateStrategy;

@Value.Enclosing
@Value.Immutable
@JsonSerialize(as = ImmutableCloudApplicationExtended.class)
@JsonDeserialize(as = ImmutableCloudApplicationExtended.class)
public interface CloudApplicationExtended extends CloudApplication {

    @Nullable
    String getModuleName();

    List<String> getIdleUris();

    Map<String, Map<String, Object>> getBindingParameters();

    List<CloudTask> getTasks();

    List<CloudRoute> getRoutes();

    List<ServiceKeyToInject> getServiceKeysToInject();

    List<String> getDomains();

    @Nullable
    RestartParameters getRestartParameters();

    @Nullable
    DockerInfo getDockerInfo();

    @Nullable
    AttributeUpdateStrategy getAttributesUpdateStrategy();
    
    @Value.Immutable
    @JsonSerialize(as = ImmutableAttributeUpdateStrategy.class)
    @JsonDeserialize(as = ImmutableAttributeUpdateStrategy.class)
    interface AttributeUpdateStrategy {

        @Value.Default
        default boolean shouldKeepExistingEnv() {
            return false;
        }

        @Value.Default
        default boolean shouldKeepExistingServiceBindings() {
            return false;
        }

        @Value.Default
        default boolean shouldKeepExistingRoutes() {
            return false;
        }

    }

}
