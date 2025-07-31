package org.cloudfoundry.multiapps.controller.client.lib.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.multiapps.common.AllowNulls;
import org.cloudfoundry.multiapps.common.Nullable;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudRoute;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudTask;
import org.cloudfoundry.multiapps.controller.client.facade.domain.DockerInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Staging;
import org.immutables.value.Value;

@Value.Enclosing
@Value.Immutable
@JsonSerialize(as = ImmutableCloudApplicationExtended.class)
@JsonDeserialize(as = ImmutableCloudApplicationExtended.class)
public abstract class CloudApplicationExtended extends CloudApplication {

    @Nullable
    public abstract String getModuleName();

    @Value.Default
    public int getMemory() {
        return 0;
    }

    @Value.Default
    public int getDiskQuota() {
        return 0;
    }

    @Value.Default
    public int getInstances() {
        return 1;
    }

    @Nullable
    public abstract Staging getStaging();

    public abstract Set<CloudRoute> getRoutes();

    public abstract List<String> getServices();

    @AllowNulls
    public abstract Map<String, String> getEnv();

    public abstract Set<CloudRoute> getIdleRoutes();

    public abstract Map<String, BindingDetails> getBindingParameters();

    public abstract List<CloudTask> getTasks();

    public abstract List<ServiceKeyToInject> getServiceKeysToInject();

    @Nullable
    public abstract RestartParameters getRestartParameters();

    @Nullable
    public abstract DockerInfo getDockerInfo();

    @Nullable
    public abstract AttributeUpdateStrategy getAttributesUpdateStrategy();

    @Value.Immutable
    @JsonSerialize(as = ImmutableCloudApplicationExtended.ImmutableAttributeUpdateStrategy.class)
    @JsonDeserialize(as = ImmutableCloudApplicationExtended.ImmutableAttributeUpdateStrategy.class)
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
