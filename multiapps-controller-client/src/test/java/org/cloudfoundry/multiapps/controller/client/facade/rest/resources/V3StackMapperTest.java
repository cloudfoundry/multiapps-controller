package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudStack;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class V3StackMapperTest {

    private static final String GUID_STRING = "3725a721-6e3f-4b6e-8f2b-1c2d3e4f5a6b";

    @Test
    void testToCloudStackMapsAllFields() {
        V3Stack stack = new V3Stack(GUID_STRING, "cflinuxfs4", "Ubuntu-based stack", "2026-08-04T10:15:30Z", "2026-08-04T10:16:30Z", null);

        CloudStack result = V3StackMapper.toCloudStack(stack);

        Assertions.assertEquals("cflinuxfs4", result.getName());
        Assertions.assertEquals("Ubuntu-based stack", result.getDescription());
        Assertions.assertEquals(GUID_STRING, result.getGuid()
                                                   .toString());
    }

    @Test
    void testToCloudStackWithNullGuidReturnsNullGuid() {
        V3Stack stack = new V3Stack(null, "cflinuxfs4", null, null, null, null);

        Assertions.assertNull(V3StackMapper.toCloudStack(stack)
                                           .getMetadata()
                                           .getGuid());
    }

}
