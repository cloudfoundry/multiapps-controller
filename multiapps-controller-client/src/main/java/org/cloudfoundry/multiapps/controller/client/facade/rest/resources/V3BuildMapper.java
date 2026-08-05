package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudBuild;
import org.cloudfoundry.multiapps.controller.client.facade.domain.DropletInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudBuild;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudBuild.ImmutableCreatedBy;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudBuild.ImmutablePackageInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableDropletInfo;

/**
 * Maps the {@link V3Build} wire model to the project's {@link CloudBuild} domain object. Mirrors the OSS {@code RawCloudBuild} adapter
 * field-for-field, so both client implementations yield identical domain objects.
 */
public final class V3BuildMapper {

    private V3BuildMapper() {
    }

    public static CloudBuild toCloudBuild(V3Build build) {
        return ImmutableCloudBuild.builder()
                                  .metadata(V3ResourceMappers.parseMetadata(build.guid(), build.createdAt(), build.updatedAt()))
                                  .createdBy(parseCreatedBy(build.createdBy()))
                                  .packageInfo(parsePackageInfo(build.inputPackage()))
                                  .dropletInfo(parseDropletInfo(build.droplet()))
                                  .state(parseState(build.state()))
                                  .error(build.error())
                                  .build();
    }

    private static CloudBuild.CreatedBy parseCreatedBy(V3Build.V3CreatedBy createdBy) {
        if (createdBy == null) {
            return null;
        }
        return ImmutableCreatedBy.builder()
                                 .guid(V3ResourceMappers.parseNullableGuid(createdBy.guid()))
                                 .name(createdBy.name())
                                 .build();
    }

    private static CloudBuild.PackageInfo parsePackageInfo(V3Build.V3PackageReference inputPackage) {
        if (inputPackage == null) {
            return null;
        }
        return ImmutablePackageInfo.of(V3ResourceMappers.parseNullableGuid(inputPackage.guid()));
    }

    private static DropletInfo parseDropletInfo(V3Build.V3DropletReference droplet) {
        if (droplet == null || droplet.guid() == null) {
            return null;
        }
        return ImmutableDropletInfo.builder()
                                   .guid(V3ResourceMappers.parseNullableGuid(droplet.guid()))
                                   .build();
    }

    private static CloudBuild.State parseState(String state) {
        return state == null ? null : CloudBuild.State.valueOf(state);
    }

}
