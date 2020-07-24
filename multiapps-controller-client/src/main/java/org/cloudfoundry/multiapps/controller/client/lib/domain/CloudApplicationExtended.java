package org.cloudfoundry.multiapps.controller.client.lib.domain;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.cloudfoundry.client.lib.domain.CloudTask;
import org.cloudfoundry.client.lib.domain.DockerInfo;
import org.cloudfoundry.multiapps.common.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Enclosing
@Value.Immutable
@JsonSerialize(as = ImmutableCloudApplicationExtended.class)
@JsonDeserialize(as = ImmutableCloudApplicationExtended.class)
public abstract class CloudApplicationExtended extends CloudApplication {

    @Nullable
    public abstract String getModuleName();

    public abstract List<String> getIdleUris();

    public abstract Map<String, Map<String, Object>> getBindingParameters();

    public abstract List<CloudTask> getTasks();

    public abstract List<CloudRoute> getRoutes();

    public abstract List<ServiceKeyToInject> getServiceKeysToInject();

    public abstract List<String> getDomains();

    @Nullable
    public abstract RestartParameters getRestartParameters();

    @Nullable
    public abstract DockerInfo getDockerInfo();

    @Nullable
    public abstract AttributeUpdateStrategy getAttributesUpdateStrategy();

    @Value.Immutable
    @JsonSerialize(as = ImmutableCloudApplicationExtended.AttributeUpdateStrategy.class)
    @JsonDeserialize(as = ImmutableCloudApplicationExtended.AttributeUpdateStrategy.class)
    public interface AttributeUpdateStrategy {

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
