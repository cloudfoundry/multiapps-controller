package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudMetadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceInstance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;

class ServiceInstancesV3OperationsTest {

    private static final String GUID_STRING = "3725a721-6e3f-4b6e-8f2b-1c2d3e4f5a6b";
    private static final UUID SPACE_GUID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Mock
    private CloudControllerV3Client cc;
    @Mock
    private CloudSpace target;
    @Mock
    private CloudMetadata targetMetadata;

    private ServiceInstancesV3Operations operations;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        Mockito.when(target.getMetadata())
               .thenReturn(targetMetadata);
        Mockito.when(targetMetadata.getGuid())
               .thenReturn(SPACE_GUID);
        operations = new ServiceInstancesV3Operations(cc, target);
    }

    @Test
    void testGetRequiredServiceInstanceGuidReturnsGuidWhenFound() {
        mockServiceInstanceList(getManagedInstance("my-service"));

        UUID result = operations.getRequiredServiceInstanceGuid("my-service");

        Assertions.assertEquals(GUID_STRING, result.toString());
    }

    @Test
    void testGetRequiredServiceInstanceGuidThrowsWhenNotFound() {
        mockServiceInstanceList();

        CloudOperationException thrown = Assertions.assertThrows(CloudOperationException.class,
                                                                 () -> operations.getRequiredServiceInstanceGuid("missing"));

        Assertions.assertEquals(HttpStatus.NOT_FOUND, thrown.getStatusCode());
    }

    @Test
    void testGetServiceInstanceWithoutAuxiliaryContentReturnsNullWhenNotRequiredAndMissing() {
        mockServiceInstanceList();

        Assertions.assertNull(operations.getServiceInstanceWithoutAuxiliaryContent("missing", false));
    }

    @Test
    void testGetServiceInstanceWithoutAuxiliaryContentThrowsWhenRequiredAndMissing() {
        mockServiceInstanceList();

        CloudOperationException thrown = Assertions.assertThrows(CloudOperationException.class,
                                                                 () -> operations.getServiceInstanceWithoutAuxiliaryContent("missing"));

        Assertions.assertEquals(HttpStatus.NOT_FOUND, thrown.getStatusCode());
    }

    @Test
    void testGetServiceInstanceWithoutAuxiliaryContentMapsWhenFound() {
        mockServiceInstanceList(getManagedInstance("my-service"));

        CloudServiceInstance result = operations.getServiceInstanceWithoutAuxiliaryContent("my-service");

        Assertions.assertEquals("my-service", result.getName());
    }

    @Test
    void testGetServiceInstanceNameReturnsNameFromGet() {
        Mockito.when(cc.get(ArgumentMatchers.contains("/v3/service_instances/"), ArgumentMatchers.eq(V3ServiceInstance.class)))
               .thenReturn(getManagedInstance("my-service"));

        Assertions.assertEquals("my-service", operations.getServiceInstanceName(SPACE_GUID));
    }

    @Test
    void testGetServiceInstanceNameReturnsNullWhenGetReturnsNull() {
        Mockito.when(cc.get(ArgumentMatchers.anyString(), ArgumentMatchers.eq(V3ServiceInstance.class)))
               .thenReturn(null);

        Assertions.assertNull(operations.getServiceInstanceName(SPACE_GUID));
    }

    private void mockServiceInstanceList(V3ServiceInstance... instances) {
        Mockito.when(cc.list(ArgumentMatchers.anyString(),
                             ArgumentMatchers.<ParameterizedTypeReference<V3ListResponse<V3ServiceInstance>>> any()))
               .thenReturn(List.of(instances));
    }

    private static V3ServiceInstance getManagedInstance(String name) {
        return new V3ServiceInstance(GUID_STRING, name, "managed", null, null, null, null, null, null, null);
    }

}
