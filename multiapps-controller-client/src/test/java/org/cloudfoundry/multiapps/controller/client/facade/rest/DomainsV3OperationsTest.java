package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudDomain;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudOrganization;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudMetadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudOrganization;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;

class DomainsV3OperationsTest {

    private static final String GUID_STRING = "3725a721-6e3f-4b6e-8f2b-1c2d3e4f5a6b";
    private static final String ORG_GUID = "11111111-2222-3333-4444-555555555555";
    private static final String DOMAIN_GUID = "22222222-2222-2222-2222-222222222222";
    private static final String JOB_GUID = "66666666-6666-6666-6666-666666666666";
    private static final String DOMAIN_NAME = "example.com";

    @Mock
    private CloudControllerV3Client cc;
    @Mock
    private CloudSpace target;

    private DomainsV3Operations operations;

    private MockControllerClientFactory factory;
    private DomainsV3Operations serverOperations;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        operations = new DomainsV3Operations(cc, target);
        factory = MockControllerClientFactory.create();
        serverOperations = new DomainsV3Operations(factory.client(), target);
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

    @Test
    void testAddDomainCreatesWhenAbsent() {
        Mockito.when(target.getOrganization())
               .thenReturn(getOrganizationWithGuid(ORG_GUID));

        stubEmptyDomainLookup(DOMAIN_NAME);

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/domains"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andRespond(MockRestResponseCreators.withSuccess(getDomainResourceJson(DOMAIN_GUID, DOMAIN_NAME),
                                                                MediaType.APPLICATION_JSON));

        serverOperations.addDomain(DOMAIN_NAME);

        factory.verify();
    }

    @Test
    void testAddDomainSkipsCreationWhenDomainAlreadyExists() {
        stubDomainLookupWithResult(DOMAIN_NAME, DOMAIN_GUID);

        serverOperations.addDomain(DOMAIN_NAME);

        factory.verify();
    }

    @Test
    void testAddDomainThrowsWhenOrganizationMissing() {
        stubEmptyDomainLookup(DOMAIN_NAME);

        Assertions.assertThrows(CloudOperationException.class, () -> serverOperations.addDomain(DOMAIN_NAME));
    }

    @Test
    void testAddDomainThrowsWhenSpaceNotProvided() {
        DomainsV3Operations noSpacedDomainOperations = new DomainsV3Operations(factory.client(), null);

        Assertions.assertThrows(IllegalArgumentException.class, () -> noSpacedDomainOperations.addDomain(DOMAIN_NAME));
    }

    @Test
    void testDeleteDomainDeletesFoundDomain() {
        stubDomainLookupWithResult(DOMAIN_NAME, DOMAIN_GUID);
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/domains/" + DOMAIN_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.DELETE))
               .andRespond(MockRestResponseCreators.withStatus(HttpStatus.ACCEPTED)
                                                   .location(URI.create(MockControllerClientFactory.BASE_URL + "/v3/jobs/" + JOB_GUID)));

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/jobs/" + JOB_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"guid\":\"" + JOB_GUID + "\",\"state\":\"COMPLETE\"}",
                                                                MediaType.APPLICATION_JSON));

        serverOperations.deleteDomain(DOMAIN_NAME);

        factory.verify();
    }

    @Test
    void testDeleteDomainThrowsWhenDomainNotFound() {
        stubEmptyDomainLookup(DOMAIN_NAME);

        Assertions.assertThrows(CloudOperationException.class, () -> serverOperations.deleteDomain(DOMAIN_NAME));
    }

    @Test
    void testDeleteDomainThrowsWhenSpaceNotProvided() {
        DomainsV3Operations noSpaceOperations = new DomainsV3Operations(factory.client(), null);

        Assertions.assertThrows(IllegalArgumentException.class, () -> noSpaceOperations.deleteDomain(DOMAIN_NAME));
    }

    @Test
    void testGetDefaultDomainReturnsMappedDomain() {
        Mockito.when(target.getOrganization())
               .thenReturn(getOrganizationWithGuid(ORG_GUID));

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(
                   MockControllerClientFactory.BASE_URL + "/v3/organizations/" + ORG_GUID + "/domains/default"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(getDomainResourceJson(DOMAIN_GUID, DOMAIN_NAME),
                                                                MediaType.APPLICATION_JSON));

        CloudDomain result = serverOperations.getDefaultDomain();

        Assertions.assertEquals(DOMAIN_NAME, result.getName());
        factory.verify();
    }

    @Test
    void testGetDomainsForOrganizationReturnsMappedDomains() {
        Mockito.when(target.getOrganization())
               .thenReturn(getOrganizationWithGuid(ORG_GUID));

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(
                   MockControllerClientFactory.BASE_URL + "/v3/organizations/" + ORG_GUID + "/domains?per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(
                   "{\"resources\":[" + getDomainResourceJson(DOMAIN_GUID, DOMAIN_NAME) + "]}", MediaType.APPLICATION_JSON));

        List<CloudDomain> result = serverOperations.getDomainsForOrganization();

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(DOMAIN_NAME, result.getFirst()
                                                   .getName());
        factory.verify();
    }

    @Test
    void testGetDomainsForOrganizationReturnsEmptyWhenNoDomains() {
        Mockito.when(target.getOrganization())
               .thenReturn(getOrganizationWithGuid(ORG_GUID));

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(
                   MockControllerClientFactory.BASE_URL + "/v3/organizations/" + ORG_GUID + "/domains?per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[]}", MediaType.APPLICATION_JSON));

        Assertions.assertTrue(serverOperations.getDomainsForOrganization()
                                              .isEmpty());
        factory.verify();
    }

    @Test
    void testGetDomainsForOrganizationThrowsWhenSpaceNotProvided() {
        DomainsV3Operations noSpaceOperations = new DomainsV3Operations(factory.client(), null);

        Assertions.assertThrows(IllegalArgumentException.class, noSpaceOperations::getDomainsForOrganization);
    }

    private void stubEmptyDomainLookup(String name) {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(
                   MockControllerClientFactory.BASE_URL + "/v3/domains?names=" + name + "&per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[]}", MediaType.APPLICATION_JSON));
    }

    private void stubDomainLookupWithResult(String name, String domainGuid) {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(
                   MockControllerClientFactory.BASE_URL + "/v3/domains?names=" + name + "&per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(
                   "{\"resources\":[" + getDomainResourceJson(domainGuid, name) + "]}", MediaType.APPLICATION_JSON));
    }

    private static CloudOrganization getOrganizationWithGuid(String guid) {
        return ImmutableCloudOrganization.builder()
                                         .metadata(ImmutableCloudMetadata.of(UUID.fromString(guid)))
                                         .name("my-org")
                                         .build();
    }

    private static String getDomainResourceJson(String guid, String name) {
        return "{\"guid\":\"" + guid + "\",\"name\":\"" + name + "\"}";
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
