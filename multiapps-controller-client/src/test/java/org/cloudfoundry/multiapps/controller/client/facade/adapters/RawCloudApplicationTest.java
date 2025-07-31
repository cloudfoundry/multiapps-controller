package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableLifecycle;
import org.cloudfoundry.client.v3.BuildpackData;
import org.cloudfoundry.client.v3.DockerData;
import org.cloudfoundry.client.v3.Lifecycle;
import org.cloudfoundry.client.v3.LifecycleType;
import org.cloudfoundry.client.v3.applications.ApplicationResource;
import org.cloudfoundry.client.v3.applications.ApplicationState;
import org.junit.jupiter.api.Test;

class RawCloudApplicationTest {

    private static final String STATE = "STARTED";
    private static final String BUILDPACK = "ruby_buildpack";
    private static final String STACK_NAME = "cflinuxfs3";
    private static final String SPACE_NAME = "test";
    private static final CloudSpace SPACE = ImmutableCloudSpace.builder()
                                                               .name(SPACE_NAME)
                                                               .build();
    private static final String EXPECTED_BUILDPACK = "ruby_buildpack";
    private static final String BUILDPACK_URL = "custom-buildpack-url";

    private static final String EXPECTED_STACK = "cflinuxfs3";
    private static final CloudApplication.State EXPECTED_STATE = CloudApplication.State.STARTED;

    @Test
    void testDeriveForBuildpackApp() {
        RawCloudEntityTest.testDerive(buildApplication(buildBuildpackLifecycle()), buildRawApplication(buildBuildpackLifecycleResource()));
    }

    @Test
    void testDeriveForDockerApp() {
        RawCloudEntityTest.testDerive(buildApplication(buildDockerLifecycle()), buildRawApplication(buildDockerLifecycleResource()));
    }

    @Test
    void testDeriveForCnbApp() {
        RawCloudEntityTest.testDerive(buildApplication(buildCnbLifecycle()), buildRawApplication(buildCnbLifecycleResource()));
    }

    private static CloudApplication buildApplication(org.cloudfoundry.multiapps.controller.client.facade.domain.Lifecycle lifecycle) {
        return ImmutableCloudApplication.builder()
                                        .metadata(RawCloudEntityTest.EXPECTED_METADATA_V3)
                                        .v3Metadata(RawCloudEntityTest.V3_METADATA)
                                        .name(RawCloudEntityTest.NAME)
                                        .state(EXPECTED_STATE)
                                        .lifecycle(lifecycle)
                                        .space(SPACE)
                                        .build();
    }

    private static org.cloudfoundry.multiapps.controller.client.facade.domain.Lifecycle buildBuildpackLifecycle() {
        return ImmutableLifecycle.builder()
                                 .type(org.cloudfoundry.multiapps.controller.client.facade.domain.LifecycleType.BUILDPACK)
                                 .data(buildLifecycleData(EXPECTED_BUILDPACK))
                                 .build();
    }

    private org.cloudfoundry.multiapps.controller.client.facade.domain.Lifecycle buildCnbLifecycle() {
        return ImmutableLifecycle.builder()
                                 .type(org.cloudfoundry.multiapps.controller.client.facade.domain.LifecycleType.CNB)
                                 .data(buildLifecycleData(BUILDPACK_URL))
                                 .build();
    }

    private static org.cloudfoundry.multiapps.controller.client.facade.domain.Lifecycle buildDockerLifecycle() {
        return ImmutableLifecycle.builder()
                                 .type(org.cloudfoundry.multiapps.controller.client.facade.domain.LifecycleType.DOCKER)
                                 .data(Map.of())
                                 .build();
    }

    private static Map<String, Object> buildLifecycleData(String buildpack) {
        return Map.of("buildpacks", List.of(buildpack), "stack", EXPECTED_STACK);
    }

    private static RawCloudApplication buildRawApplication(Lifecycle lifecycle) {
        return ImmutableRawCloudApplication.builder()
                                           .application(buildApplicationResource(lifecycle))
                                           .space(SPACE)
                                           .build();
    }

    private static ApplicationResource buildApplicationResource(Lifecycle lifecycle) {
        return ApplicationResource.builder()
                                  .metadata(RawCloudEntityTest.V3_METADATA)
                                  .createdAt(RawCloudEntityTest.CREATED_AT_STRING)
                                  .updatedAt(RawCloudEntityTest.UPDATED_AT_STRING)
                                  .state(ApplicationState.valueOf(STATE))
                                  .id(RawCloudEntityTest.GUID.toString())
                                  .lifecycle(lifecycle)
                                  .name("foo")
                                  .build();
    }

    private static Lifecycle buildBuildpackLifecycleResource() {
        return Lifecycle.builder()
                        .type(LifecycleType.BUILDPACK)
                        .data(BuildpackData.builder()
                                           .buildpack(BUILDPACK)
                                           .stack(STACK_NAME)
                                           .build())
                        .build();
    }

    private Lifecycle buildCnbLifecycleResource() {
        return Lifecycle.builder()
                        .type(LifecycleType.CNB)
                        .data(BuildpackData.builder()
                                           .buildpack(BUILDPACK_URL)
                                           .stack(STACK_NAME)
                                           .build())
                        .build();
    }

    private static Lifecycle buildDockerLifecycleResource() {
        return Lifecycle.builder()
                        .type(LifecycleType.DOCKER)
                        .data(DockerData.builder()
                                        .build())
                        .build();
    }

}
