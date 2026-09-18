package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.Set;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.UserRole;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;

class RolesV3OperationsTest {

    private static final UUID SPACE_GUID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID USER_GUID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final String ROLES_URI = "/v3/roles?per_page=5000&space_guids=" + SPACE_GUID + "&user_guids=" + USER_GUID;

    @Mock
    private CloudSpace target;

    private MockControllerClientFactory factory;
    private RolesV3Operations operations;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        factory = MockControllerClientFactory.create();
        operations = new RolesV3Operations(factory.client());
    }

    @Test
    void testGetUserRolesReturnsMappedRoles() {
        String json = "{\"resources\":[{\"guid\":\"r1\",\"type\":\"space_developer\"},"
            + "{\"guid\":\"r2\",\"type\":\"space_manager\"}]}";

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + ROLES_URI))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(json, MediaType.APPLICATION_JSON));

        Set<UserRole> result = operations.getUserRolesBySpaceAndUser(SPACE_GUID, USER_GUID);

        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.contains(UserRole.SPACE_DEVELOPER));
        Assertions.assertTrue(result.contains(UserRole.SPACE_MANAGER));
        factory.verify();
    }

    @Test
    void testGetUserRolesDeduplicatesRepeatedRoleTypes() {
        String json = "{\"resources\":[{\"guid\":\"r1\",\"type\":\"space_developer\"},"
            + "{\"guid\":\"r2\",\"type\":\"space_developer\"}]}";
        
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + ROLES_URI))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(json, MediaType.APPLICATION_JSON));

        Set<UserRole> result = operations.getUserRolesBySpaceAndUser(SPACE_GUID, USER_GUID);

        Assertions.assertEquals(1, result.size());
        Assertions.assertTrue(result.contains(UserRole.SPACE_DEVELOPER));
        factory.verify();
    }

    @Test
    void testGetUserRolesReturnsEmptySetWhenNoRoles() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + ROLES_URI))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[]}", MediaType.APPLICATION_JSON));

        Set<UserRole> result = operations.getUserRolesBySpaceAndUser(SPACE_GUID, USER_GUID);

        Assertions.assertTrue(result.isEmpty());
        factory.verify();
    }

    @Test
    void testGetUserRolesThrowsOnUnknownRoleType() {
        String json = "{\"resources\":[{\"guid\":\"r1\",\"type\":\"galactic_overlord\"}]}";
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + ROLES_URI))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(json, MediaType.APPLICATION_JSON));

        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> operations.getUserRolesBySpaceAndUser(SPACE_GUID, USER_GUID));
    }

    @Test
    void testGetUserRolesThrowsOnServerError() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + ROLES_URI))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        Assertions.assertThrows(CloudOperationException.class,
                                () -> operations.getUserRolesBySpaceAndUser(SPACE_GUID, USER_GUID));
    }

}
