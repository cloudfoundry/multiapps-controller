package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.List;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudStack;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Stack;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;

class StacksV3OperationsTest {

    private static final String GUID_STRING = "3725a721-6e3f-4b6e-8f2b-1c2d3e4f5a6b";

    @Mock
    private CloudControllerV3Client cc;

    private StacksV3Operations operations;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        operations = new StacksV3Operations(cc);
    }

    @Test
    void testGetStackReturnsMappedStackWhenFound() {
        mockStackList(new V3Stack(GUID_STRING, "cflinuxfs4", "Ubuntu-based stack", null, null, null));

        CloudStack result = operations.getStack("cflinuxfs4");

        Assertions.assertEquals("cflinuxfs4", result.getName());
        Assertions.assertEquals("Ubuntu-based stack", result.getDescription());
    }

    @Test
    void testGetStackRequiredThrowsWhenNotFound() {
        mockStackList();

        CloudOperationException thrown = Assertions.assertThrows(CloudOperationException.class, () -> operations.getStack("missing"));

        Assertions.assertEquals(HttpStatus.NOT_FOUND, thrown.getStatusCode());
    }

    @Test
    void testGetStackNotRequiredReturnsNullWhenNotFound() {
        mockStackList();

        Assertions.assertNull(operations.getStack("missing", false));
    }

    @Test
    void testGetStacksMapsAllResults() {
        mockStackList(new V3Stack(GUID_STRING, "cflinuxfs4", null, null, null, null),
                      new V3Stack(GUID_STRING, "cflinuxfs3", null, null, null, null));

        List<CloudStack> result = operations.getStacks();

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("cflinuxfs4", result.getFirst()
                                                    .getName());
    }

    @Test
    void testGetStacksQueriesStacksEndpointWithPageSize() {
        mockStackList();

        operations.getStacks();

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(cc)
               .list(uriCaptor.capture(), ArgumentMatchers.<ParameterizedTypeReference<V3ListResponse<V3Stack>>> any());
        Assertions.assertTrue(uriCaptor.getValue()
                                       .contains("/v3/stacks"), uriCaptor.getValue());
        Assertions.assertTrue(uriCaptor.getValue()
                                       .contains("per_page="), uriCaptor.getValue());
    }

    private void mockStackList(V3Stack... stacks) {
        Mockito.when(cc.list(ArgumentMatchers.anyString(),
                             ArgumentMatchers.<ParameterizedTypeReference<V3ListResponse<V3Stack>>> any()))
               .thenReturn(List.of(stacks));
    }

}
