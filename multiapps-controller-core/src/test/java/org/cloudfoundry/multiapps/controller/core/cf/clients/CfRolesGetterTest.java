package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.client.facade.CloudCredentials;
import org.cloudfoundry.multiapps.controller.client.facade.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CfRolesGetterTest extends CustomControllerClientBaseTest {

    private CfRolesGetter client;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        Mockito.when(applicationConfiguration.getVersion())
               .thenReturn("1.0.0");
        Mockito.when(webClientFactory.getWebClient(Mockito.any(CloudCredentials.class)))
               .thenReturn(webClient);

        client = new CfRolesGetter(applicationConfiguration, webClientFactory, new CloudCredentials("user", "pass"));
    }

    @Test
    void testGetRolesBuildsCorrectUri() {
        stubWebClientToReturnResponse();

        UUID spaceGuid = UUID.randomUUID();
        UUID userGuid = UUID.randomUUID();
        client.getRoles(spaceGuid, userGuid);

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(requestHeadersUriSpec)
               .uri(uriCaptor.capture());

        String capturedUri = uriCaptor.getValue();
        assertTrue(capturedUri.startsWith("/v3/roles?"), "URI should start with /v3/roles?");
        assertTrue(capturedUri.contains("space_guids=" + spaceGuid), "URI should contain the space guid");
        assertTrue(capturedUri.contains("user_guids=" + userGuid), "URI should contain the user guid");
    }

    @Test
    void testGetRolesUriContainsAllRoleTypes() {
        stubWebClientToReturnResponse();

        UUID spaceGuid = UUID.randomUUID();
        UUID userGuid = UUID.randomUUID();
        client.getRoles(spaceGuid, userGuid);

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(requestHeadersUriSpec)
               .uri(uriCaptor.capture());

        String capturedUri = uriCaptor.getValue();
        String expectedTypesFilter = Arrays.stream(UserRole.values())
                                           .map(UserRole::getName)
                                           .collect(Collectors.joining(","));
        assertTrue(capturedUri.contains("types=" + expectedTypesFilter), "URI should contain all UserRole types as a filter");
    }

    @Test
    void testGetRolesWithEmptyResponseReturnsEmptySet() {
        stubWebClientToReturnResponse();

        UUID spaceGuid = UUID.randomUUID();
        UUID userGuid = UUID.randomUUID();
        Set<UserRole> result = client.getRoles(spaceGuid, userGuid);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetRolesReturnsSingleRole() {
        String responseJson = buildRolesResponse("space_developer");
        stubWebClientToReturnResponse(responseJson);

        UUID spaceGuid = UUID.randomUUID();
        UUID userGuid = UUID.randomUUID();
        Set<UserRole> result = client.getRoles(spaceGuid, userGuid);

        assertEquals(1, result.size());
        assertTrue(result.contains(UserRole.SPACE_DEVELOPER));
    }

    @Test
    void testGetRolesReturnsMultipleRoles() {
        String responseJson = buildRolesResponse("space_developer", "space_manager", "space_auditor");
        stubWebClientToReturnResponse(responseJson);

        UUID spaceGuid = UUID.randomUUID();
        UUID userGuid = UUID.randomUUID();
        Set<UserRole> result = client.getRoles(spaceGuid, userGuid);

        assertEquals(3, result.size());
        assertTrue(result.contains(UserRole.SPACE_DEVELOPER));
        assertTrue(result.contains(UserRole.SPACE_MANAGER));
        assertTrue(result.contains(UserRole.SPACE_AUDITOR));
    }

    @Test
    void testGetRolesReturnsOrganizationRoles() {
        String responseJson = buildRolesResponse("organization_manager", "organization_auditor");
        stubWebClientToReturnResponse(responseJson);

        UUID spaceGuid = UUID.randomUUID();
        UUID userGuid = UUID.randomUUID();
        Set<UserRole> result = client.getRoles(spaceGuid, userGuid);

        assertEquals(2, result.size());
        assertTrue(result.contains(UserRole.ORGANIZATION_MANAGER));
        assertTrue(result.contains(UserRole.ORGANIZATION_AUDITOR));
    }

    @Test
    void testGetRolesDeduplicatesDuplicateRoles() {
        String responseJson = buildRolesResponse("space_developer", "space_developer");
        stubWebClientToReturnResponse(responseJson);

        UUID spaceGuid = UUID.randomUUID();
        UUID userGuid = UUID.randomUUID();
        Set<UserRole> result = client.getRoles(spaceGuid, userGuid);

        assertEquals(1, result.size(), "Duplicate roles should be deduplicated in the EnumSet");
        assertTrue(result.contains(UserRole.SPACE_DEVELOPER));
    }

    @Test
    void testGetRolesPaginatedResponseFollowsAllPages() {
        String page1Json = buildRolesResponseWithPagination(new String[] { "space_developer" }, "/v3/roles?page=2");
        String page2Json = buildRolesResponse("space_manager");

        stubWebClientToReturnResponse(page1Json, page2Json);

        UUID spaceGuid = UUID.randomUUID();
        UUID userGuid = UUID.randomUUID();
        Set<UserRole> result = client.getRoles(spaceGuid, userGuid);

        assertEquals(2, result.size());
        assertTrue(result.contains(UserRole.SPACE_DEVELOPER));
        assertTrue(result.contains(UserRole.SPACE_MANAGER));
    }

    @Test
    void testGetRolesMakesExactlyOneHttpCallForSinglePage() {
        stubWebClientToReturnResponse();

        UUID spaceGuid = UUID.randomUUID();
        UUID userGuid = UUID.randomUUID();
        client.getRoles(spaceGuid, userGuid);

        Mockito.verify(webClient, Mockito.times(1))
               .get();
    }

    @Test
    void testGetRolesMakesTwoHttpCallsForTwoPages() {
        String page1Json = buildRolesResponseWithPagination(new String[] { "space_developer" }, "/v3/roles?page=2");
        String page2Json = buildRolesResponse("space_auditor");

        stubWebClientToReturnResponse(page1Json, page2Json);

        UUID spaceGuid = UUID.randomUUID();
        UUID userGuid = UUID.randomUUID();
        client.getRoles(spaceGuid, userGuid);

        Mockito.verify(webClient, Mockito.times(2))
               .get();
    }

    @Test
    void testGetRolesReturnsAllSpaceRoles() {
        String responseJson = buildRolesResponse("space_developer", "space_manager", "space_auditor");
        stubWebClientToReturnResponse(responseJson);

        UUID spaceGuid = UUID.randomUUID();
        UUID userGuid = UUID.randomUUID();
        Set<UserRole> result = client.getRoles(spaceGuid, userGuid);

        assertTrue(result.contains(UserRole.SPACE_DEVELOPER));
        assertTrue(result.contains(UserRole.SPACE_MANAGER));
        assertTrue(result.contains(UserRole.SPACE_AUDITOR));
    }

    @Test
    void testGetRolesReturnsMixedSpaceAndOrgRoles() {
        String responseJson = buildRolesResponse("space_developer", "organization_manager", "organization_user");
        stubWebClientToReturnResponse(responseJson);

        UUID spaceGuid = UUID.randomUUID();
        UUID userGuid = UUID.randomUUID();
        Set<UserRole> result = client.getRoles(spaceGuid, userGuid);

        assertEquals(3, result.size());
        assertTrue(result.contains(UserRole.SPACE_DEVELOPER));
        assertTrue(result.contains(UserRole.ORGANIZATION_MANAGER));
        assertTrue(result.contains(UserRole.ORGANIZATION_USER));
    }

    private String buildRolesResponse(String... roleTypes) {
        return buildRolesResponseWithPagination(roleTypes, null);
    }

    private String buildRolesResponseWithPagination(String[] roleTypes, String nextPageHref) {
        String nextPage = nextPageHref == null ? "null" : "{\"href\":\"" + nextPageHref + "\"}";
        StringBuilder resources = new StringBuilder();
        for (int i = 0; i < roleTypes.length; i++) {
            if (i > 0) {
                resources.append(",");
            }
            resources.append("{")
                     .append("\"guid\":\"")
                     .append(UUID.randomUUID())
                     .append("\",")
                     .append("\"type\":\"")
                     .append(roleTypes[i])
                     .append("\",")
                     .append("\"created_at\":\"2024-01-01T00:00:00Z\",")
                     .append("\"updated_at\":\"2024-01-01T00:00:00Z\"")
                     .append("}");
        }
        return "{\"resources\":[" + resources + "],\"pagination\":{\"next\":" + nextPage + "}}";
    }
}

