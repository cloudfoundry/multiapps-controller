package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import java.util.UUID;

import org.cloudfoundry.client.v3.BuildpackData;
import org.cloudfoundry.client.v3.Lifecycle;
import org.cloudfoundry.client.v3.LifecycleType;
import org.cloudfoundry.client.v3.Relationship;
import org.cloudfoundry.client.v3.builds.Build;
import org.cloudfoundry.client.v3.builds.BuildResource;
import org.cloudfoundry.client.v3.builds.BuildState;
import org.cloudfoundry.client.v3.builds.CreatedBy;
import org.cloudfoundry.client.v3.builds.Droplet;
import org.junit.jupiter.api.Test;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudBuild;
import org.cloudfoundry.multiapps.controller.client.facade.domain.DropletInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudBuild;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableDropletInfo;

class RawCloudBuildTest {

    private static final String PACKAGE_GUID_STRING = "d0bd1437-f112-48cd-b777-6b160cf2fff0";
    private static final String DROPLET_GUID_STRING = "47e172c9-bfb2-41df-bf3f-850108c241ef";
    private static final Droplet DROPLET = buildTestDroplet();
    private static final String CREATED_BY_GUID_STRING = "37964fd8-a234-4c04-af31-c3fe5e477bed";
    private static final String CREATED_BY_NAME = "admin";
    private static final LifecycleType LIFECYCLE_TYPE = LifecycleType.BUILDPACK;
    private static final BuildState STATE = BuildState.FAILED;
    private static final String ERROR = "blabla";

    private static final UUID PACKAGE_GUID = UUID.fromString(PACKAGE_GUID_STRING);
    private static final UUID DROPLET_GUID = UUID.fromString(DROPLET_GUID_STRING);
    private static final UUID CREATED_BY_GUID = UUID.fromString(CREATED_BY_GUID_STRING);

    private static final CloudBuild.State EXPECTED_STATE = CloudBuild.State.FAILED;
    private static final DropletInfo EXPECTED_DROPLET_INFO = buildExpectedDropletInfo();

    private static CloudBuild buildExpectedBuild() {
        return buildExpectedBuild(null);
    }

    private static CloudBuild buildExpectedBuild(DropletInfo dropletInfo) {
        return ImmutableCloudBuild.builder()
                                  .metadata(RawCloudEntityTest.EXPECTED_METADATA_PARSED_FROM_V3_RESOURCE)
                                  .createdBy(buildExpectedCreatedBy())
                                  .dropletInfo(dropletInfo)
                                  .packageInfo(buildExpectedPackageInfo())
                                  .state(EXPECTED_STATE)
                                  .error(ERROR)
                                  .build();
    }

    private static CloudBuild.CreatedBy buildExpectedCreatedBy() {
        return ImmutableCloudBuild.ImmutableCreatedBy.builder()
                                                     .guid(CREATED_BY_GUID)
                                                     .name(CREATED_BY_NAME)
                                                     .build();
    }

    private static DropletInfo buildExpectedDropletInfo() {
        return ImmutableDropletInfo.builder()
                                   .guid(DROPLET_GUID)
                                   .build();
    }

    private static CloudBuild.PackageInfo buildExpectedPackageInfo() {
        return ImmutableCloudBuild.ImmutablePackageInfo.builder()
                                                       .guid(PACKAGE_GUID)
                                                       .build();
    }

    private static Build buildTestResource(Droplet droplet) {
        return BuildResource.builder()
                            .id(RawCloudEntityTest.GUID_STRING)
                            .createdAt(RawCloudEntityTest.CREATED_AT_STRING)
                            .updatedAt(RawCloudEntityTest.UPDATED_AT_STRING)
                            .inputPackage(buildTestPackage())
                            .createdBy(buildTestCreatedBy())
                            .lifecycle(buildTestLifecycle())
                            .droplet(droplet)
                            .state(STATE)
                            .error(ERROR)
                            .build();
    }

    private static Relationship buildTestPackage() {
        return Relationship.builder()
                           .id(PACKAGE_GUID_STRING)
                           .build();
    }

    private static Droplet buildTestDroplet() {
        return Droplet.builder()
                      .id(DROPLET_GUID_STRING)
                      .build();
    }

    private static CreatedBy buildTestCreatedBy() {
        return CreatedBy.builder()
                        .id(CREATED_BY_GUID_STRING)
                        .name(CREATED_BY_NAME)
                        .email(CREATED_BY_NAME)
                        .build();
    }

    private static Lifecycle buildTestLifecycle() {
        return Lifecycle.builder()
                        .data(buildTestLifecycleData())
                        .type(LIFECYCLE_TYPE)
                        .build();
    }

    private static BuildpackData buildTestLifecycleData() {
        return BuildpackData.builder()
                            .build();
    }

    @Test
    void testDeriveWithoutDroplet() {
        RawCloudEntityTest.testDerive(buildExpectedBuild(), buildRawBuild());
    }

    @Test
    void testDerive() {
        RawCloudEntityTest.testDerive(buildExpectedBuild(EXPECTED_DROPLET_INFO), buildRawBuild(DROPLET));
    }

    private RawCloudBuild buildRawBuild() {
        return buildRawBuild(null);
    }

    private RawCloudBuild buildRawBuild(Droplet droplet) {
        return ImmutableRawCloudBuild.of(buildTestResource(droplet));
    }

}
