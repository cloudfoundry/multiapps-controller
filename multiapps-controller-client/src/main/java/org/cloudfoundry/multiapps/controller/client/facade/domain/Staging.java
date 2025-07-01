package org.cloudfoundry.multiapps.controller.client.facade.domain;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.multiapps.controller.client.facade.Nullable;
import org.cloudfoundry.multiapps.controller.client.facade.SkipNulls;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableStaging.class)
@JsonDeserialize(as = ImmutableStaging.class)
public interface Staging {
    /**
     * @return The buildpacks, or empty to use the default buildpack detected based on application content
     */
    @SkipNulls
    List<String> getBuildpacks();

    /**
     * @return The start command to use
     */
    @Nullable
    String getCommand();

    /**
     * @return Raw, free-form information regarding a detected buildpack, or null if no detected buildpack was resolved. For example, if the
     * application is stopped, the detected buildpack may be null.
     */
    @Nullable
    String getDetectedBuildpack();

    /**
     * @return the health check timeout value
     */
    @Nullable
    Integer getHealthCheckTimeout();

    /**
     * @return health check type
     */
    @Nullable
    String getHealthCheckType();

    /**
     * @return health check http endpoint value
     */
    @Nullable
    String getHealthCheckHttpEndpoint();

    /**
     * @return boolean value to see if ssh is enabled
     */
    @Nullable
    Boolean isSshEnabled();

    /**
     * Retrieves the application features map. The map contains feature names as keys and their enabled/disabled state as Boolean values.
     * This allows specifying which features should be enabled or disabled for the application during staging.
     *
     * @return a map of application features and their enabled/disabled state
     */
    @SkipNulls
    Map<String, Boolean> getAppFeatures();

    /**
     * @return the stack to use when staging the application, or null to use the default stack
     */
    @Nullable
    String getStackName();

    @Nullable
    DockerInfo getDockerInfo();

    @Nullable
    Integer getInvocationTimeout();

    @Nullable
    LifecycleType getLifecycleType();

    default String getBuildpack() {
        return getBuildpacks().isEmpty() ? null : getBuildpacks().get(0);
    }

}
