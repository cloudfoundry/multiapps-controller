package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.List;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudDomain;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Domain;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Domain.V3DomainRelationships;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Domain.V3RelationshipData;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Domain.V3ToOneRelationship;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.ParameterizedTypeReference;

class DomainsV3OperationsTest {

    private static final String GUID_STRING = "3725a721-6e3f-4b6e-8f2b-1c2d3e4f5a6b";
    private static final String ORG_GUID = "11111111-2222-3333-4444-555555555555";

    @Mock
    private CloudControllerV3Client cc;
    @Mock
    private CloudSpace target;

    private DomainsV3Operations operations;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        operations = new DomainsV3Operations(cc, target);
    }

    @Test
    void testGetDomainsMapsAllResults() {
        mockDomainList(getSharedDomain("shared.example.com"), getPrivateDomain("private.example.com"));

        List<CloudDomain> result = operations.getDomains();

        Assertions.assertEquals(2, result.size());
    }

    @Test
    void testGetSharedDomainsReturnsOnlyNonPrivate() {
        mockDomainList(getSharedDomain("shared.example.com"), getPrivateDomain("private.example.com"));

        List<CloudDomain> result = operations.getSharedDomains();

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("shared.example.com", result.getFirst()
                                                            .getName());
    }

    @Test
    void testGetPrivateDomainsReturnsOnlyPrivate() {
        mockDomainList(getSharedDomain("shared.example.com"), getPrivateDomain("private.example.com"));

        List<CloudDomain> result = operations.getPrivateDomains();

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("private.example.com", result.getFirst()
                                                             .getName());
    }

    @Test
    void testGetSharedDomainsReturnsEmptyWhenNoDomains() {
        mockDomainList();

        Assertions.assertTrue(operations.getSharedDomains()
                                        .isEmpty());
    }

    private void mockDomainList(V3Domain... domains) {
        Mockito.when(cc.list(ArgumentMatchers.anyString(),
                             ArgumentMatchers.<ParameterizedTypeReference<V3ListResponse<V3Domain>>> any()))
               .thenReturn(List.of(domains));
    }

    private static V3Domain getSharedDomain(String name) {
        return new V3Domain(GUID_STRING, name, null, null, null, null);
    }

    private static V3Domain getPrivateDomain(String name) {
        V3DomainRelationships relationships = new V3DomainRelationships(new V3ToOneRelationship(new V3RelationshipData(ORG_GUID)));
        return new V3Domain(GUID_STRING, name, null, null, null, relationships);
    }

}
