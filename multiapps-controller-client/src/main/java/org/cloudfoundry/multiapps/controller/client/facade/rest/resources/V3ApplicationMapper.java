package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.Collections;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableLifecycle;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Lifecycle;
import org.cloudfoundry.multiapps.controller.client.facade.domain.LifecycleType;

/**
 * Maps the {@link V3Application} wire model to the project's {@link CloudApplication} domain object. Mirrors the OSS
 * {@code RawCloudApplication} adapter field-for-field, so both client implementations yield identical domain objects.
 */
public final class V3ApplicationMapper {

    private static final String BUILDPACKS = "buildpacks";
    private static final String STACK = "stack";

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
            return Collections.emptyMap();
        }
        return Map.of(BUILDPACKS, data.buildpacks() == null ? Collections.emptyList() : data.buildpacks(),
                      STACK, data.stack() == null ? "" : data.stack());
    }

}
