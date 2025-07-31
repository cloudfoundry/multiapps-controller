package org.cloudfoundry.multiapps.controller.client.facade.domain;

import java.util.UUID;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.multiapps.controller.client.facade.Nullable;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudBuild.ImmutableCreatedBy;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudBuild.ImmutablePackageInfo;

@Value.Enclosing
@Value.Immutable
@JsonSerialize(as = ImmutableCloudBuild.class)
@JsonDeserialize(as = ImmutableCloudBuild.class)
public abstract class CloudBuild extends CloudEntity implements Derivable<CloudBuild> {

    @Nullable
    public abstract State getState();

    @Nullable
    public abstract CreatedBy getCreatedBy();

    @Nullable
    public abstract DropletInfo getDropletInfo();

    @Nullable
    public abstract PackageInfo getPackageInfo();

    @Nullable
    public abstract String getError();

    @Override
    public CloudBuild derive() {
        return this;
    }

    public enum State {
        FAILED, STAGED, STAGING
    }

    @Value.Immutable
    @JsonSerialize(as = ImmutablePackageInfo.class)
    @JsonDeserialize(as = ImmutablePackageInfo.class)
    public interface PackageInfo {

        @Nullable
        @Value.Parameter
        UUID getGuid();

    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableCreatedBy.class)
    @JsonDeserialize(as = ImmutableCreatedBy.class)
    public interface CreatedBy {

        @Nullable
        UUID getGuid();

        @Nullable
        String getName();

    }

}
