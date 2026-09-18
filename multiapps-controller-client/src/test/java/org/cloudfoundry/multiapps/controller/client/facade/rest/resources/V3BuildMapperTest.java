package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudBuild;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Build.V3CreatedBy;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Build.V3DropletReference;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Build.V3PackageReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class V3BuildMapperTest {

    private static final String GUID_STRING = "3725a721-6e3f-4b6e-8f2b-1c2d3e4f5a6b";
    private static final String USER_GUID = "11111111-2222-3333-4444-555555555555";
    private static final String PACKAGE_GUID = "22222222-3333-4444-5555-666666666666";
    private static final String DROPLET_GUID = "33333333-4444-5555-6666-777777777777";

    @Test
    void testToCloudBuildMapsAllFields() {
        V3Build build = new V3Build(GUID_STRING, null, null, "STAGED", null, new V3CreatedBy(USER_GUID, "admin"),
                                    new V3PackageReference(PACKAGE_GUID), new V3DropletReference(DROPLET_GUID));

        CloudBuild result = V3BuildMapper.toCloudBuild(build);

        Assertions.assertEquals(CloudBuild.State.STAGED, result.getState());
        Assertions.assertEquals(USER_GUID, result.getCreatedBy()
                                                 .getGuid()
                                                 .toString());
        Assertions.assertEquals("admin", result.getCreatedBy()
                                               .getName());
        Assertions.assertEquals(PACKAGE_GUID, result.getPackageInfo()
                                                    .getGuid()
                                                    .toString());
        Assertions.assertEquals(DROPLET_GUID, result.getDropletInfo()
                                                    .getGuid()
                                                    .toString());
    }

    @Test
    void testToCloudBuildNullCreatedByPackageAndDropletReturnNulls() {
        V3Build build = new V3Build(GUID_STRING, null, null, "STAGING", "boom", null, null, null);

        CloudBuild result = V3BuildMapper.toCloudBuild(build);

        Assertions.assertNull(result.getCreatedBy());
        Assertions.assertNull(result.getPackageInfo());
        Assertions.assertNull(result.getDropletInfo());
        Assertions.assertEquals("boom", result.getError());
    }

    @Test
    void testToCLoudBuildDropletWithNullGuidReturnsNullDropletInfo() {
        V3Build build = new V3Build(GUID_STRING, null, null, "FAILED", null, null, null, new V3DropletReference(null));

        Assertions.assertNull(V3BuildMapper.toCloudBuild(build)
                                           .getDropletInfo());
    }

    @Test
    void testToCloudBuildNullStateReturnsNullState() {
        V3Build build = new V3Build(GUID_STRING, null, null, null, null, null, null, null);

        Assertions.assertNull(V3BuildMapper.toCloudBuild(build)
                                           .getState());
    }

}
