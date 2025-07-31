package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import java.util.Optional;

import org.cloudfoundry.client.v3.Relationship;
import org.cloudfoundry.client.v3.builds.Build;
import org.cloudfoundry.client.v3.builds.BuildState;
import org.cloudfoundry.client.v3.builds.CreatedBy;
import org.cloudfoundry.client.v3.builds.Droplet;
import org.immutables.value.Value;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudBuild;
import org.cloudfoundry.multiapps.controller.client.facade.domain.DropletInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudBuild;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudBuild.ImmutableCreatedBy;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudBuild.ImmutablePackageInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableDropletInfo;

@Value.Immutable
public abstract class RawCloudBuild extends RawCloudEntity<CloudBuild> {

    private static CloudBuild.CreatedBy parseCreatedBy(Build buildResource) {
        CreatedBy createdBy = buildResource.getCreatedBy();
        return ImmutableCreatedBy.builder()
                                 .guid(parseNullableGuid(createdBy.getId()))
                                 .name(createdBy.getName())
                                 .build();
    }

    private static CloudBuild.PackageInfo parsePackageInfo(Build buildResource) {
        Relationship packageRelationship = buildResource.getInputPackage();
        String packageId = packageRelationship.getId();
        return ImmutablePackageInfo.of(parseNullableGuid(packageId));
    }

    private static DropletInfo parseDropletInfo(Build buildResource) {
        Droplet droplet = buildResource.getDroplet();
        return Optional.ofNullable(droplet)
                       .map(Droplet::getId)
                       .map(RawCloudEntity::parseNullableGuid)
                       .map(dropletGuid -> ImmutableDropletInfo.builder()
                                                               .guid(dropletGuid)
                                                               .build())
                       .orElse(null);
    }

    private static CloudBuild.State parseState(BuildState state) {
        return parseEnum(state, CloudBuild.State.class);
    }

    @Value.Parameter
    public abstract Build getResource();

    @Override
    public CloudBuild derive() {
        Build resource = getResource();
        return ImmutableCloudBuild.builder()
                                  .metadata(parseResourceMetadata(resource))
                                  .createdBy(parseCreatedBy(resource))
                                  .packageInfo(parsePackageInfo(resource))
                                  .dropletInfo(parseDropletInfo(resource))
                                  .state(parseState(resource.getState()))
                                  .error(resource.getError())
                                  .build();
    }

}
