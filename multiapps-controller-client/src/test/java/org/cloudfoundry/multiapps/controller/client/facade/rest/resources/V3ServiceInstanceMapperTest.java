package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.List;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceInstanceType;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceOperation;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceInstance.V3LastOperation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class V3ServiceInstanceMapperTest {

    private static final String GUID_STRING = "3725a721-6e3f-4b6e-8f2b-1c2d3e4f5a6b";

    @Test
    void testToCloudServiceInstanceMapsAllFields() {
        V3ServiceInstance instance = new V3ServiceInstance(GUID_STRING, "my-service", "managed", null, null, List.of("tag1", "tag2"),
                                                           "syslog://drain", new V3LastOperation("create", "succeeded", "done"), null,
                                                           null);

        CloudServiceInstance result = V3ServiceInstanceMapper.toCloudServiceInstance(instance, "my-plan", "my-label");

        Assertions.assertEquals("my-service", result.getName());
        Assertions.assertEquals("my-plan", result.getPlan());
        Assertions.assertEquals("my-label", result.getLabel());
        Assertions.assertEquals(ServiceInstanceType.MANAGED, result.getType());
        Assertions.assertEquals(List.of("tag1", "tag2"), result.getTags());
        Assertions.assertEquals("syslog://drain", result.getSyslogDrainUrl());
        Assertions.assertEquals(ServiceOperation.Type.CREATE, result.getLastOperation()
                                                                    .getType());
        Assertions.assertEquals(ServiceOperation.State.SUCCEEDED, result.getLastOperation()
                                                                        .getState());
    }

    @Test
    void testToCloudServiceInstanceWithUserProvidedType() {
        V3ServiceInstance instance = buildInstanceWithType("user-provided");

        Assertions.assertEquals(ServiceInstanceType.USER_PROVIDED, V3ServiceInstanceMapper.toCloudServiceInstance(instance, null, null)
                                                                                          .getType());
    }

    @Test
    void testToCloudServiceInstanceWithNullTypeReturnsNullType() {
        V3ServiceInstance instance = buildInstanceWithType(null);

        Assertions.assertNull(V3ServiceInstanceMapper.toCloudServiceInstance(instance, null, null)
                                                     .getType());
    }

    @Test
    void testToCloudServiceInstanceWithUnknownTypeReturnsNullType() {
        V3ServiceInstance instance = buildInstanceWithType("bogus");

        Assertions.assertNull(V3ServiceInstanceMapper.toCloudServiceInstance(instance, null, null)
                                                     .getType());
    }

    @Test
    void testToCloudServiceInstanceNullTagsReturnEmptyList() {
        V3ServiceInstance instance = new V3ServiceInstance(GUID_STRING, "s", "managed", null, null, null, null, null, null, null);

        Assertions.assertTrue(V3ServiceInstanceMapper.toCloudServiceInstance(instance, null, null)
                                                     .getTags()
                                                     .isEmpty());
    }

    @Test
    void testToCloudServiceInstanceNullLastOperationReturnsNullOperation() {
        V3ServiceInstance instance = new V3ServiceInstance(GUID_STRING, "s", "managed", null, null, null, null, null, null, null);

        Assertions.assertNull(V3ServiceInstanceMapper.toCloudServiceInstance(instance, null, null)
                                                     .getLastOperation());
    }

    @Test
    void testToCloudServiceInstanceLastOperationWithNullTypeReturnsNullOperation() {
        V3ServiceInstance instance = new V3ServiceInstance(GUID_STRING, "s", "managed", null, null, null, null,
                                                           new V3LastOperation(null, "succeeded", "d"), null, null);

        Assertions.assertNull(V3ServiceInstanceMapper.toCloudServiceInstance(instance, null, null)
                                                     .getLastOperation());
    }

    @Test
    void testToCloudServiceInstanceWithoutAuxiliaryContentExcludesPlanAndOperation() {
        V3ServiceInstance instance = new V3ServiceInstance(GUID_STRING, "my-service", "managed", null, null, List.of("t"), "syslog",
                                                           new V3LastOperation("create", "succeeded", "d"), null, null);

        CloudServiceInstance result = V3ServiceInstanceMapper.toCloudServiceInstanceWithoutAuxiliaryContent(instance);

        Assertions.assertEquals("my-service", result.getName());
        Assertions.assertEquals(List.of("t"), result.getTags());
        Assertions.assertNull(result.getPlan());
        Assertions.assertNull(result.getLastOperation());
        Assertions.assertNull(result.getType());
    }

    private static V3ServiceInstance buildInstanceWithType(String type) {
        return new V3ServiceInstance(GUID_STRING, "s", type, null, null, null, null, null, null, null);
    }

}
