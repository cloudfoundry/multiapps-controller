package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import org.cloudfoundry.multiapps.controller.client.facade.domain.BitsData;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudPackage;
import org.cloudfoundry.multiapps.controller.client.facade.domain.DockerData;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Status;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Package.V3Checksum;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Package.V3PackageData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class V3PackageMapperTest {

    private static final String GUID_STRING = "3725a721-6e3f-4b6e-8f2b-1c2d3e4f5a6b";

    @Test
    void testToCloudPackageMapsBitsPackage() {
        V3PackageData data = new V3PackageData(new V3Checksum("sha256", "abc123"), "no error", null, null, null);
        V3Package resource = new V3Package(GUID_STRING, "bits", "READY", null, null, data);

        CloudPackage result = V3PackageMapper.toCloudPackage(resource);

        Assertions.assertEquals(Status.READY, result.getStatus());
        Assertions.assertEquals(CloudPackage.Type.BITS, result.getType());
        BitsData bitsData = (BitsData) result.getData();
        Assertions.assertEquals("sha256", bitsData.getChecksum()
                                                  .getAlgorithm());
        Assertions.assertEquals("abc123", bitsData.getChecksum()
                                                  .getValue());
        Assertions.assertEquals("no error", bitsData.getError());
    }

    @Test
    void testToCloudPackageMapsDockerPackage() {
        V3PackageData data = new V3PackageData(null, null, "my/image:latest", "user", "pass");
        V3Package resource = new V3Package(GUID_STRING, "docker", "READY", null, null, data);

        CloudPackage result = V3PackageMapper.toCloudPackage(resource);

        Assertions.assertEquals(CloudPackage.Type.DOCKER, result.getType());
        DockerData dockerData = (DockerData) result.getData();
        Assertions.assertEquals("my/image:latest", dockerData.getImage());
        Assertions.assertEquals("user", dockerData.getUsername());
    }

    @Test
    void testToCloudPackageNullStateReturnsNullStatus() {
        V3Package resource = new V3Package(GUID_STRING, "bits", null, null, null, null);

        Assertions.assertNull(V3PackageMapper.toCloudPackage(resource)
                                             .getStatus());
    }

    @Test
    void testToCloudPackageNullTypeReturnsNullType() {
        V3Package resource = new V3Package(GUID_STRING, null, "READY", null, null, null);

        Assertions.assertNull(V3PackageMapper.toCloudPackage(resource)
                                             .getType());
    }

    @Test
    void testToCloudPackageBitsPackageWithNullDataReturnsEmptyBitsData() {
        V3Package resource = new V3Package(GUID_STRING, "bits", "READY", null, null, null);

        BitsData bitsData = (BitsData) V3PackageMapper.toCloudPackage(resource)
                                                      .getData();

        Assertions.assertNull(bitsData.getChecksum());
        Assertions.assertNull(bitsData.getError());
    }

    @Test
    void testToCloudPackageBitsPackageWithNullChecksumReturnsNullChecksum() {
        V3PackageData data = new V3PackageData(null, "err", null, null, null);
        V3Package resource = new V3Package(GUID_STRING, "bits", "READY", null, null, data);

        BitsData bitsData = (BitsData) V3PackageMapper.toCloudPackage(resource)
                                                      .getData();

        Assertions.assertNull(bitsData.getChecksum());
        Assertions.assertEquals("err", bitsData.getError());
    }

}
