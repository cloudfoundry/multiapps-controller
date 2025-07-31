package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import org.cloudfoundry.client.v3.Checksum;
import org.cloudfoundry.client.v3.packages.BitsData;
import org.cloudfoundry.client.v3.packages.DockerData;
import org.cloudfoundry.client.v3.packages.Package;
import org.cloudfoundry.client.v3.packages.PackageType;
import org.immutables.value.Value;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudPackage;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableBitsData;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudPackage;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableDockerData;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Status;

@Value.Immutable
public abstract class RawCloudPackage extends RawCloudEntity<CloudPackage> {

    @Value.Parameter
    public abstract Package getResource();

    @Override
    public CloudPackage derive() {
        Package resource = getResource();
        return ImmutableCloudPackage.builder()
                                    .metadata(parseResourceMetadata(resource))
                                    .status(parseStatus(resource))
                                    .data(parseData(resource))
                                    .type(parseType(resource))
                                    .build();
    }

    private static Status parseStatus(Package resource) {
        return parseEnum(resource.getState(), Status.class);
    }

    private static CloudPackage.PackageData parseData(Package resource) {
        if (resource.getType() == PackageType.BITS) {
            return parseBitsData((BitsData) resource.getData());
        }
        return parseDockerData((DockerData) resource.getData());
    }

    private static CloudPackage.PackageData parseBitsData(BitsData data) {
        return ImmutableBitsData.builder()
                                .checksum(parseBitsChecksum(data.getChecksum()))
                                .error(data.getError())
                                .build();
    }

    private static org.cloudfoundry.multiapps.controller.client.facade.domain.BitsData.Checksum parseBitsChecksum(Checksum checksum) {
        if (checksum == null) {
            return null;
        }
        return ImmutableBitsData.ImmutableChecksum.builder()
                                                  .algorithm(checksum.getType()
                                                                     .toString())
                                                  .value(checksum.getValue())
                                                  .build();
    }

    private static CloudPackage.PackageData parseDockerData(DockerData data) {
        return ImmutableDockerData.builder()
                                  .image(data.getImage())
                                  .username(data.getUsername())
                                  .password(data.getPassword())
                                  .build();
    }

    private static CloudPackage.Type parseType(Package resource) {
        return parseEnum(resource.getType(), CloudPackage.Type.class);
    }

}
