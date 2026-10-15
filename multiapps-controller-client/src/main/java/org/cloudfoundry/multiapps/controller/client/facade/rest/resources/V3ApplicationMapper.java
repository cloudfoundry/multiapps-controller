package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.Constants;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableLifecycle;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Lifecycle;
import org.cloudfoundry.multiapps.controller.client.facade.domain.LifecycleType;

public final class V3ApplicationMapper {

    private V3ApplicationMapper() {
    }

    public static CloudApplication toCloudApplication(V3Application app, CloudSpace space) {
        return ImmutableCloudApplication.builder()
                                        .metadata(V3ResourceMappers.parseMetadata(app.guid(), app.createdAt(), app.updatedAt()))
                                        .v3Metadata(V3ResourceMappers.toV3Metadata(app.metadata()))
                                        .name(app.name())
                                        .state(parseState(app.state()))
                                        .lifecycle(parseLifecycle(app.lifecycle()))
                                        .space(space)
                                        .build();
    }

    private static CloudApplication.State parseState(String state) {
        return state == null ? null : CloudApplication.State.valueOf(state);
    }

    private static Lifecycle parseLifecycle(V3Application.V3Lifecycle lifecycle) {
        if (lifecycle == null || lifecycle.type() == null) {
            return null;
        }

        return ImmutableLifecycle.builder()
                                 .type(LifecycleType.valueOf(lifecycle.type()
                                                                      .toUpperCase()))
                                 .data(extractLifecycleData(lifecycle.data()))
                                 .build();
    }

    private static Map<String, Object> extractLifecycleData(V3Application.V3LifecycleData data) {
        if (data == null) {
            return Map.of();
        }

        return Map.of(V3Fields.BUILDPACKS, data.buildpacks() == null ? List.of() : data.buildpacks(), V3Fields.STACK,
                      data.stack() == null ? Constants.EMPTY_STRING : data.stack());
    }

}
