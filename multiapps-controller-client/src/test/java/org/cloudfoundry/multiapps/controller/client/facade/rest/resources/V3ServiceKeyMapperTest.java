package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.Map;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceCredentialBindingOperation;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceBinding.V3LastOperation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class V3ServiceKeyMapperTest {

    private static final String GUID_STRING = "3725a721-6e3f-4b6e-8f2b-1c2d3e4f5a6b";

    @Test
    void testToCloudServiceKeyMapsNameAndCredentials() {
        V3ServiceBinding key = new V3ServiceBinding(GUID_STRING, "my-key", "key", null, null, null, null, null);

        CloudServiceKey result = V3ServiceKeyMapper.toCloudServiceKey(key, null, Map.of("username", "u", "password", "p"));

        Assertions.assertEquals("my-key", result.getName());
        Assertions.assertEquals(Map.of("username", "u", "password", "p"), result.getCredentials());
        Assertions.assertNull(result.getServiceKeyOperation());
    }

    @Test
    void testToCloudServiceKeyWithoutCredentialsReturnsNullCredentials() {
        V3ServiceBinding key = new V3ServiceBinding(GUID_STRING, "my-key", "key", null, null, null, null, null);

        Assertions.assertNull(V3ServiceKeyMapper.toCloudServiceKey(key, null)
                                                .getCredentials());
    }

    @Test
    void testToCloudServiceKeyMapsLastOperation() {
        V3LastOperation lastOperation = new V3LastOperation("create", "succeeded", "done", "2026-08-04T10:15:30Z",
                                                            "2026-08-04T10:16:30Z");
        V3ServiceBinding key = new V3ServiceBinding(GUID_STRING, "my-key", "key", null, null, lastOperation, null, null);

        CloudServiceKey result = V3ServiceKeyMapper.toCloudServiceKey(key, null, null);

        Assertions.assertEquals(ServiceCredentialBindingOperation.Type.CREATE, result.getServiceKeyOperation()
                                                                                     .getType());
        Assertions.assertEquals(ServiceCredentialBindingOperation.State.SUCCEEDED, result.getServiceKeyOperation()
                                                                                         .getState());
        Assertions.assertEquals("done", result.getServiceKeyOperation()
                                              .getDescription());
    }

}
