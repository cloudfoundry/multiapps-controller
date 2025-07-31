package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import java.util.Collections;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Derivable;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableLifecycle;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Lifecycle;
import org.cloudfoundry.multiapps.controller.client.facade.domain.LifecycleType;
import org.cloudfoundry.client.v3.BuildpackData;
import org.cloudfoundry.client.v3.CnbData;
import org.cloudfoundry.client.v3.LifecycleData;
import org.cloudfoundry.client.v3.applications.Application;
import org.cloudfoundry.client.v3.applications.ApplicationState;
import org.immutables.value.Value;

@Value.Immutable
public abstract class RawCloudApplication extends RawCloudEntity<CloudApplication> {

    public static final String BUILDPACKS = "buildpacks";
    public static final String STACK = "stack";

    public abstract Application getApplication();

    public abstract Derivable<CloudSpace> getSpace();

    @Override
    public CloudApplication derive() {
        Application app = getApplication();
        return ImmutableCloudApplication.builder()
                                        .metadata(parseResourceMetadata(app))
                                        .v3Metadata(app.getMetadata())
                                        .name(app.getName())
                                        .state(parseState(app.getState()))
                                        .lifecycle(parseLifecycle(app.getLifecycle()))
                                        .space(getSpace().derive())
                                        .build();
    }

    private static CloudApplication.State parseState(ApplicationState state) {
        return CloudApplication.State.valueOf(state.getValue());
    }

    private static Lifecycle parseLifecycle(org.cloudfoundry.client.v3.Lifecycle lifecycle) {
        Map<String, Object> data = extractLifecycleData(lifecycle.getData());

        return ImmutableLifecycle.builder()
                                 .type(LifecycleType.valueOf(lifecycle.getType()
                                                                      .toString()
                                                                      .toUpperCase()))
                                 .data(data)
                                 .build();
    }

    private static Map<String, Object> extractLifecycleData(LifecycleData lifecycleData) {
        if (lifecycleData instanceof BuildpackData buildpackData) {
            return Map.of(
                BUILDPACKS, buildpackData.getBuildpacks(),
                STACK, buildpackData.getStack());
        } else if (lifecycleData instanceof CnbData cnbData) {
            return Map.of(
                BUILDPACKS, cnbData.getBuildpacks(),
                STACK, cnbData.getStack());
        } else {
            return Collections.emptyMap();
        }
    }

}
