package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.client.facade.CloudCredentials;
import org.cloudfoundry.multiapps.controller.client.facade.domain.UserRole;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CfRolesGetterTest {

    @Mock
    private WebClientFactory webClientFactory;
    @Mock
    private ApplicationConfiguration applicationConfiguration;
    @Mock
    private WebClient webClient;

    @SuppressWarnings("rawtypes")
    private final WebClient.RequestHeadersUriSpec requestHeadersUriSpec = Mockito.mock(WebClient.RequestHeadersUriSpec.class);
    @SuppressWarnings("rawtypes")
    private final WebClient.RequestHeadersSpec requestHeadersSpec = Mockito.mock(WebClient.RequestHeadersSpec.class);
    @Mock
    private WebClient.ResponseSpec responseSpec;

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
        stubWebClientToReturnEmptyPage();

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
        stubWebClientToReturnEmptyPage();

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
        stubWebClientToReturnEmptyPage();

        UUID spaceGuid = UUID.randomUUID();
        UUID userGuid = UUID.randomUUID();
        Set<UserRole> result = client.getRoles(spaceGuid, userGuid);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetRolesReturnsSingleRole() {
        String responseJson = buildRolesResponse("space_developer");
        stubWebClientToReturn(responseJson);

        UUID spaceGuid = UUID.randomUUID();
        UUID userGuid = UUID.randomUUID();
        Set<UserRole> result = client.getRoles(spaceGuid, userGuid);

        assertEquals(1, result.size());
        assertTrue(result.contains(UserRole.SPACE_DEVELOPER));
    }

    @Test
    void testGetRolesReturnsMultipleRoles() {
        String responseJson = buildRolesResponse("space_developer", "space_manager", "space_auditor");
        stubWebClientToReturn(responseJson);

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
        stubWebClientToReturn(responseJson);

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
        stubWebClientToReturn(responseJson);

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

        stubWebClientToReturnSequentially(page1Json, page2Json);

        UUID spaceGuid = UUID.randomUUID();
        UUID userGuid = UUID.randomUUID();
        Set<UserRole> result = client.getRoles(spaceGuid, userGuid);

        assertEquals(2, result.size());
        assertTrue(result.contains(UserRole.SPACE_DEVELOPER));
        assertTrue(result.contains(UserRole.SPACE_MANAGER));
    }

    @Test
    void testGetRolesMakesExactlyOneHttpCallForSinglePage() {
        stubWebClientToReturnEmptyPage();

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

        stubWebClientToReturnSequentially(page1Json, page2Json);

        UUID spaceGuid = UUID.randomUUID();
        UUID userGuid = UUID.randomUUID();
        client.getRoles(spaceGuid, userGuid);

        Mockito.verify(webClient, Mockito.times(2))
               .get();
    }

    @Test
    void testGetRolesReturnsAllSpaceRoles() {
        String responseJson = buildRolesResponse("space_developer", "space_manager", "space_auditor");
        stubWebClientToReturn(responseJson);

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
        stubWebClientToReturn(responseJson);

        UUID spaceGuid = UUID.randomUUID();
        UUID userGuid = UUID.randomUUID();
        Set<UserRole> result = client.getRoles(spaceGuid, userGuid);

        assertEquals(3, result.size());
        assertTrue(result.contains(UserRole.SPACE_DEVELOPER));
        assertTrue(result.contains(UserRole.ORGANIZATION_MANAGER));
        assertTrue(result.contains(UserRole.ORGANIZATION_USER));
    }

    @SuppressWarnings("unchecked")
    private void stubWebClientToReturnEmptyPage() {
        Mockito.when(webClient.get())
               .thenReturn(requestHeadersUriSpec);
        Mockito.when(requestHeadersUriSpec.uri(Mockito.anyString()))
               .thenReturn(requestHeadersSpec);
        Mockito.when(requestHeadersSpec.headers(Mockito.any(Consumer.class)))
               .thenReturn(requestHeadersSpec);
        Mockito.when(requestHeadersSpec.retrieve())
               .thenReturn(responseSpec);
        Mockito.when(responseSpec.bodyToMono(String.class))
               .thenReturn(Mono.just("{\"resources\":[],\"pagination\":{\"next\":null}}"));
    }

    @SuppressWarnings("unchecked")
    private void stubWebClientToReturn(String responseJson) {
        Mockito.when(webClient.get())
               .thenReturn(requestHeadersUriSpec);
        Mockito.when(requestHeadersUriSpec.uri(Mockito.anyString()))
               .thenReturn(requestHeadersSpec);
        Mockito.when(requestHeadersSpec.headers(Mockito.any(Consumer.class)))
               .thenReturn(requestHeadersSpec);
        Mockito.when(requestHeadersSpec.retrieve())
               .thenReturn(responseSpec);
        Mockito.when(responseSpec.bodyToMono(String.class))
               .thenReturn(Mono.just(responseJson));
    }

    @SuppressWarnings("unchecked")
    private void stubWebClientToReturnSequentially(String... responses) {
        Mockito.when(webClient.get())
               .thenReturn(requestHeadersUriSpec);
        Mockito.when(requestHeadersUriSpec.uri(Mockito.anyString()))
               .thenReturn(requestHeadersSpec);
        Mockito.when(requestHeadersSpec.headers(Mockito.any(Consumer.class)))
               .thenReturn(requestHeadersSpec);
        Mockito.when(requestHeadersSpec.retrieve())
               .thenReturn(responseSpec);

        if (responses.length == 1) {
            Mockito.when(responseSpec.bodyToMono(String.class))
                   .thenReturn(Mono.just(responses[0]));
        } else {
            @SuppressWarnings("rawtypes") Mono[] remaining = new Mono[responses.length - 1];
            for (int i = 1; i < responses.length; i++) {
                remaining[i - 1] = Mono.just(responses[i]);
            }
            Mockito.when(responseSpec.bodyToMono(String.class))
                   .thenReturn(Mono.just(responses[0]), remaining);
        }
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

