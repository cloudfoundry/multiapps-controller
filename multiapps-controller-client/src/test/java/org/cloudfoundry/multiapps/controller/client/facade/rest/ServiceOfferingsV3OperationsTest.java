package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceOffering;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceOffering;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServicePlan;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.ParameterizedTypeReference;

class ServiceOfferingsV3OperationsTest {

    private static final String OFFERING_GUID = "3725a721-6e3f-4b6e-8f2b-1c2d3e4f5a6b";
    private static final String PLAN_GUID = "22222222-3333-4444-5555-666666666666";
    private static final UUID SPACE_GUID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Mock
    private CloudControllerV3Client cc;
    @Mock
    private CloudSpace target;

    private ServiceOfferingsV3Operations operations;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        operations = new ServiceOfferingsV3Operations(cc, target);
    }

    @Test
    void testGetServiceOfferingsMapsOfferingWithItsPlans() {
        Mockito.when(cc.list(ArgumentMatchers.contains("/v3/service_offerings"),
                             ArgumentMatchers.<ParameterizedTypeReference<V3ListResponse<V3ServiceOffering>>> any()))
               .thenReturn(List.of(getOffering()));
        Mockito.when(cc.list(ArgumentMatchers.contains("/v3/service_plans"),
                             ArgumentMatchers.<ParameterizedTypeReference<V3ListResponse<V3ServicePlan>>> any()))
               .thenReturn(List.of(getPlan("plan-a"), getPlan("plan-b")));

        List<CloudServiceOffering> result = operations.getServiceOfferings();

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("my-offering", result.getFirst()
                                                     .getName());
        Assertions.assertEquals(2, result.getFirst()
                                         .getServicePlans()
                                         .size());
    }

    @Test
    void testGetServiceOfferingsScopesQueryToSpaceWhenTargetHasGuid() {
        Mockito.when(target.getGuid())
               .thenReturn(SPACE_GUID);

        Mockito.when(cc.list(ArgumentMatchers.contains("/v3/service_offerings"),
                             ArgumentMatchers.<ParameterizedTypeReference<V3ListResponse<V3ServiceOffering>>> any()))
               .thenReturn(List.of());

        operations.getServiceOfferings();

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(cc)
               .list(uriCaptor.capture(), ArgumentMatchers.<ParameterizedTypeReference<V3ListResponse<V3ServiceOffering>>> any());
        Assertions.assertTrue(uriCaptor.getValue()
                                       .contains("space_guids=" + SPACE_GUID), uriCaptor.getValue());
    }

    @Test
    void testGetServiceOfferingsExcludesSpaceGuidWhenTargetGuidNull() {
        Mockito.when(target.getGuid())
               .thenReturn(null);

        Mockito.when(cc.list(ArgumentMatchers.contains("/v3/service_offerings"),
                             ArgumentMatchers.<ParameterizedTypeReference<V3ListResponse<V3ServiceOffering>>> any()))
               .thenReturn(List.of());

        operations.getServiceOfferings();

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(cc)
               .list(uriCaptor.capture(), ArgumentMatchers.<ParameterizedTypeReference<V3ListResponse<V3ServiceOffering>>> any());
        Assertions.assertFalse(uriCaptor.getValue()
                                        .contains("space_guids="), uriCaptor.getValue());
    }

    private static V3ServiceOffering getOffering() {
        return new V3ServiceOffering(OFFERING_GUID, "my-offering", "desc", true, false, null, null, null, null, null, null);
    }

    private static V3ServicePlan getPlan(String name) {
        return new V3ServicePlan(PLAN_GUID, name, null, true, "public", null, null, null, null, null);
    }

}
