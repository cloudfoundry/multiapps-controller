package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudPackage;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableBitsData;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudPackage;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableDockerData;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Status;

/**
 * Maps the {@link V3Package} wire model to the project's {@link CloudPackage} domain object. Mirrors the OSS {@code RawCloudPackage}
 * adapter field-for-field, so both client implementations yield identical domain objects.
 */
public final class V3PackageMapper {

    private V3PackageMapper() {
    }

    public static CloudPackage toCloudPackage(V3Package resource) {
        return ImmutableCloudPackage.builder()
                                    .metadata(V3ResourceMappers.parseMetadata(resource.guid(), resource.createdAt(),
                                                                              resource.updatedAt()))
                                    .status(parseStatus(resource))
                                    .data(parseData(resource))
                                    .type(parseType(resource))
                                    .build();
    }

    private static Status parseStatus(V3Package resource) {
        return resource.state() == null ? null : Status.valueOf(resource.state());
    }

    private static CloudPackage.Type parseType(V3Package resource) {
        return resource.type() == null ? null : CloudPackage.Type.from(resource.type());
    }

    private static CloudPackage.PackageData parseData(V3Package resource) {
        if (resource.type() != null && CloudPackage.Type.from(resource.type()) == CloudPackage.Type.BITS) {
            return parseBitsData(resource.data());
        }
        return parseDockerData(resource.data());
    }

    private static CloudPackage.PackageData parseBitsData(V3Package.V3PackageData data) {
        if (data == null) {
            return ImmutableBitsData.builder()
                                    .build();
        }
        return ImmutableBitsData.builder()
                                .checksum(parseBitsChecksum(data.checksum()))
                                .error(data.error())
                                .build();
    }

    private static org.cloudfoundry.multiapps.controller.client.facade.domain.BitsData.Checksum
            parseBitsChecksum(V3Package.V3Checksum checksum) {
        if (checksum == null) {
            return null;
        }
        return ImmutableBitsData.ImmutableChecksum.builder()
                                                  .algorithm(checksum.type())
                                                  .value(checksum.value())
                                                  .build();
    }

    private static CloudPackage.PackageData parseDockerData(V3Package.V3PackageData data) {
        if (data == null) {
            return null;
        }
        return ImmutableDockerData.builder()
                                  .image(data.image())
                                  .username(data.username())
                                  .password(data.password())
                                  .build();
    }

}
