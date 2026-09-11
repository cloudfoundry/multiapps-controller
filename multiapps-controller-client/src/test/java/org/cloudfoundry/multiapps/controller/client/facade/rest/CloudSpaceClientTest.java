package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;

class CloudSpaceClientTest {

    private static final String SPACE_GUID = "11111111-1111-1111-1111-111111111111";
    private static final String ORG_GUID = "22222222-2222-2222-2222-222222222222";
    private static final String SPACE_NAME = "my-space";
    private static final String ORG_NAME = "my-org";

    private MockControllerClientFactory factory;
    private CloudSpaceClient client;

    @BeforeEach
    void setUp() {
        factory = MockControllerClientFactory.create();
        client = new CloudSpaceClient(factory.client());
    }

    @Test
    void testGetSpaceByGuidReturnsMappedSpace() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/spaces/" + SPACE_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(getSpaceJson(), MediaType.APPLICATION_JSON));

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/organizations/" + ORG_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(getOrgJson(), MediaType.APPLICATION_JSON));

        CloudSpace result = client.getSpace(UUID.fromString(SPACE_GUID));

        Assertions.assertEquals(SPACE_NAME, result.getName());
        Assertions.assertEquals(UUID.fromString(SPACE_GUID), result.getGuid());
        Assertions.assertEquals(ORG_NAME, result.getOrganization()
                                                .getName());
        Assertions.assertEquals(UUID.fromString(ORG_GUID), result.getOrganization()
                                                                 .getGuid());
        factory.verify();
    }

    @Test
    void testGetSpaceByGuidThrowsWhenSpaceNotFound() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/spaces/" + SPACE_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withStatus(HttpStatus.NOT_FOUND));

        Assertions.assertThrows(CloudOperationException.class, () -> client.getSpace(UUID.fromString(SPACE_GUID)));
    }

    @Test
    void testGetSpaceByGuidThrowsWhenOrganizationNotFound() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/spaces/" + SPACE_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(getSpaceJson(), MediaType.APPLICATION_JSON));
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/organizations/" + ORG_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withStatus(HttpStatus.NOT_FOUND));

        Assertions.assertThrows(CloudOperationException.class, () -> client.getSpace(UUID.fromString(SPACE_GUID)));
    }

    @Test
    void testGetSpaceByNameReturnsMappedSpace() {
        stubOrgLookupWithResult();

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/spaces?organization_guids=" + ORG_GUID
                                                             + "&names=" + SPACE_NAME))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[" + getSpaceJson() + "]}", MediaType.APPLICATION_JSON));

        CloudSpace result = client.getSpace(ORG_NAME, SPACE_NAME);

        Assertions.assertEquals(SPACE_NAME, result.getName());
        Assertions.assertEquals(ORG_NAME, result.getOrganization()
                                                .getName());
        factory.verify();
    }

    @Test
    void testGetSpaceByNameThrowsWhenOrganizationNotFound() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/organizations?names=" + ORG_NAME))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[]}", MediaType.APPLICATION_JSON));

        Assertions.assertThrows(CloudOperationException.class, () -> client.getSpace(ORG_NAME, SPACE_NAME));
    }

    @Test
    void testGetSpaceByNameThrowsWhenSpaceNotFound() {
        stubOrgLookupWithResult();

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/spaces?organization_guids=" + ORG_GUID
                                                             + "&names=" + SPACE_NAME))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[]}", MediaType.APPLICATION_JSON));

        Assertions.assertThrows(CloudOperationException.class, () -> client.getSpace(ORG_NAME, SPACE_NAME));
    }

    private void stubOrgLookupWithResult() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/organizations?names=" + ORG_NAME))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[" + getOrgJson() + "]}", MediaType.APPLICATION_JSON));
    }

    private static String getSpaceJson() {
        return "{\"guid\":\"" + SPACE_GUID + "\",\"name\":\"" + SPACE_NAME + "\","
            + "\"relationships\":{\"organization\":{\"data\":{\"guid\":\"" + ORG_GUID + "\"}}}}";
    }

    private static String getOrgJson() {
        return "{\"guid\":\"" + ORG_GUID + "\",\"name\":\"" + ORG_NAME + "\"}";
    }

}
