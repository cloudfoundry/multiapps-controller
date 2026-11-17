package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudRoute;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudDomain;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudRoute;
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

import static org.mockito.Mockito.when;

class RoutesV3OperationsTest {

    private static final String SPACE_GUID = "11111111-1111-1111-1111-111111111111";
    private static final String DOMAIN_GUID = "22222222-2222-2222-2222-222222222222";
    private static final String ROUTE_GUID = "33333333-3333-3333-3333-333333333333";
    private static final String SECOND_ROUTE_GUID = "77777777-7777-7777-7777-777777777777";
    private static final String APP_GUID = "44444444-4444-4444-4444-444444444444";
    private static final String DESTINATION_GUID = "55555555-5555-5555-5555-555555555555";
    private static final String JOB_GUID = "66666666-6666-6666-6666-666666666666";
    private static final String DOMAIN_NAME = "example.com";

    @Mock
    private CloudSpace target;

    private MockControllerClientFactory factory;
    private RoutesV3Operations operations;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        factory = MockControllerClientFactory.create();
        operations = new RoutesV3Operations(factory.client(), target);
    }

    @Test
    void testAddRoutePostsToRoutes() {
        when(target.getGuid())
            .thenReturn(UUID.fromString(SPACE_GUID));

        String route = getRouteJson(ROUTE_GUID, "myhost", "/mypath", "myhost.example.com/mypath");

        stubDomainLookup(DOMAIN_NAME, DOMAIN_GUID);
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/routes"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andRespond(MockRestResponseCreators.withSuccess(route, MediaType.APPLICATION_JSON));

        operations.addRoute("myhost", DOMAIN_NAME, "/mypath");

        factory.verify();
    }

    @Test
    void testAddRouteThrowsWhenDomainNotFound() {
        when(target.getGuid())
            .thenReturn(UUID.fromString(SPACE_GUID));
        stubEmptyDomainLookup(DOMAIN_NAME);

        Assertions.assertThrows(CloudOperationException.class, () -> operations.addRoute("myhost", DOMAIN_NAME, "/mypath"));
    }

    @Test
    void testAddRouteThrowsWhenSpaceNotProvided() {
        RoutesV3Operations noSpaceOperations = new RoutesV3Operations(factory.client(), null);

        Assertions.assertThrows(IllegalArgumentException.class, () -> noSpaceOperations.addRoute("myhost", DOMAIN_NAME, "/mypath"));
    }

    @Test
    void testDeleteRouteDeletesFoundRoute() {
        when(target.getGuid())
            .thenReturn(UUID.fromString(SPACE_GUID));

        stubDomainLookup(DOMAIN_NAME, DOMAIN_GUID);
        stubRouteLookupWithResult();
        stubAsyncDelete(MockControllerClientFactory.BASE_URL + "/v3/routes/" + ROUTE_GUID);

        operations.deleteRoute("myhost", DOMAIN_NAME, "/mypath");

        factory.verify();
    }

    @Test
    void testDeleteRouteThrowsWhenRouteNotFound() {
        when(target.getGuid())
            .thenReturn(UUID.fromString(SPACE_GUID));

        stubDomainLookup(DOMAIN_NAME, DOMAIN_GUID);
        factory.server()
               .expect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[]}", MediaType.APPLICATION_JSON));

        Assertions.assertThrows(CloudOperationException.class, () -> operations.deleteRoute("myhost", DOMAIN_NAME, "/mypath"));
    }

    @Test
    void testDeleteRouteThrowsWhenSpaceNotProvided() {
        RoutesV3Operations noSpaceOperations = new RoutesV3Operations(factory.client(), null);

        Assertions.assertThrows(IllegalArgumentException.class, () -> noSpaceOperations.deleteRoute("myhost", DOMAIN_NAME, "/mypath"));
    }

    @Test
    void testDeleteOrphanedRoutes() {
        when(target.getGuid())
            .thenReturn(UUID.fromString(SPACE_GUID));

        stubAsyncDelete(MockControllerClientFactory.BASE_URL + "/v3/spaces/" + SPACE_GUID + "/routes?unmapped=true");

        operations.deleteOrphanedRoutes();

        factory.verify();
    }

    @Test
    void testGetRoutesReturnsMappedRoutes() {
        when(target.getGuid())
            .thenReturn(UUID.fromString(SPACE_GUID));

        String firstRoute = getRouteJson(ROUTE_GUID, "myhost", "/mypath", "myhost.example.com/mypath");

        stubDomainLookup(DOMAIN_NAME, DOMAIN_GUID);
        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/routes?per_page=50&domain_guids="
                            + DOMAIN_GUID + "&space_guids=" + SPACE_GUID, "{\"resources\":[" + firstRoute + "]}");

        List<CloudRoute> routes = operations.getRoutes(DOMAIN_NAME);

        Assertions.assertEquals(1, routes.size());
        Assertions.assertEquals("myhost", routes.getFirst()
                                                .getHost());
        factory.verify();
    }

    @Test
    void testGetRoutesReturnsMultipleMappedRoutes() {
        when(target.getGuid())
            .thenReturn(UUID.fromString(SPACE_GUID));

        String firstRoute = getRouteJson(ROUTE_GUID, "myhost", "/mypath", "myhost.example.com/mypath");
        String secondRoute = getRouteJson(SECOND_ROUTE_GUID, "otherhost", "/otherpath", "otherhost.example.com/otherpath");

        stubDomainLookup(DOMAIN_NAME, DOMAIN_GUID);
        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/routes?per_page=50&domain_guids="
                            + DOMAIN_GUID + "&space_guids=" + SPACE_GUID, "{\"resources\":[" + firstRoute + "," + secondRoute + "]}");

        List<CloudRoute> routes = operations.getRoutes(DOMAIN_NAME);

        Assertions.assertEquals(2, routes.size());
        Assertions.assertEquals(Set.of("myhost", "otherhost"), routes.stream()
                                                                     .map(CloudRoute::getHost)
                                                                     .collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of("myhost.example.com/mypath", "otherhost.example.com/otherpath"),
                                routes.stream()
                                      .map(CloudRoute::getUrl)
                                      .collect(Collectors.toSet()));
        factory.verify();
    }

    @Test
    void testGetRoutesThrowsWhenDomainNotFound() {
        when(target.getGuid())
            .thenReturn(UUID.fromString(SPACE_GUID));

        stubEmptyDomainLookup(DOMAIN_NAME);

        Assertions.assertThrows(CloudOperationException.class, () -> operations.getRoutes(DOMAIN_NAME));
    }

    @Test
    void testGetRoutesThrowsWhenSpaceNotProvided() {
        RoutesV3Operations noSpaceOperations = new RoutesV3Operations(factory.client(), null);

        Assertions.assertThrows(IllegalArgumentException.class, () -> noSpaceOperations.getRoutes(DOMAIN_NAME));
    }

    @Test
    void testGetApplicationRoutesReturnsMappedRoutes() {
        String route = getRouteJson(ROUTE_GUID, "myhost", "/mypath", "myhost.example.com/mypath");

        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/routes?per_page=50",
                        "{\"resources\":[" + route + "]}");

        List<CloudRoute> routes = operations.getApplicationRoutes(UUID.fromString(APP_GUID));

        Assertions.assertEquals(1, routes.size());
        Assertions.assertEquals("myhost.example.com/mypath", routes.getFirst()
                                                                   .getUrl());
        factory.verify();
    }

    @Test
    void testGetApplicationRoutesReturnsEmptyList() {
        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/routes?per_page=50",
                        "{\"resources\":[]}");

        List<CloudRoute> routes = operations.getApplicationRoutes(UUID.fromString(APP_GUID));

        Assertions.assertEquals(0, routes.size());
        factory.verify();
    }

    @Test
    void testUpdateApplicationRoutesAddsNewRoute() {
        when(target.getGuid())
            .thenReturn(UUID.fromString(SPACE_GUID));

        String route = getRouteJson(ROUTE_GUID, "myhost", "/mypath", "myhost.example.com/mypath");

        stubAppLookup("my-app");

        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/routes?per_page=50",
                        "{\"resources\":[]}");

        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/domains?names=" + DOMAIN_NAME + "&per_page=50",
                        getDomainJson(DOMAIN_GUID));

        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/routes?per_page=50&domain_guids="
                            + DOMAIN_GUID + "&space_guids=" + SPACE_GUID + "&hosts=myhost&paths=/mypath", "{\"resources\":[]}");

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/routes"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andRespond(MockRestResponseCreators.withSuccess(route, MediaType.APPLICATION_JSON));

        factory.stubMethod(MockControllerClientFactory.BASE_URL + "/v3/routes/" + ROUTE_GUID + "/destinations", HttpMethod.POST);

        CloudRoute newRoute = ImmutableCloudRoute.builder()
                                                 .domain(ImmutableCloudDomain.builder()
                                                                             .name(DOMAIN_NAME)
                                                                             .build())
                                                 .host("myhost")
                                                 .path("/mypath")
                                                 .url("myhost.example.com/mypath")
                                                 .build();

        operations.updateApplicationRoutes("my-app", Set.of(newRoute));

        factory.verify();
    }

    @Test
    void testUpdateApplicationRoutesRemovesOutdatedRoute() {
        when(target.getGuid())
            .thenReturn(UUID.fromString(SPACE_GUID));

        stubAppLookup("my-app");

        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/routes?per_page=50",
                        "{\"resources\":[" + getRouteWithDestinationJson() + "]}");

        factory.stubMethod(MockControllerClientFactory.BASE_URL + "/v3/routes/" + ROUTE_GUID + "/destinations/" + DESTINATION_GUID,
                           HttpMethod.DELETE);

        operations.updateApplicationRoutes("my-app", Set.of());

        factory.verify();
    }

    @Test
    void testUpdateApplicationRoutesThrowsWhenApplicationNotFound() {
        when(target.getGuid())
            .thenReturn(UUID.fromString(SPACE_GUID));

        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps?per_page=50"
                            + "&space_guids=" + SPACE_GUID + "&names=missing-app", "{\"resources\":[]}");

        Assertions.assertThrows(CloudOperationException.class, () -> operations.updateApplicationRoutes("missing-app", Set.of()));
    }

    private void stubDomainLookup(String name, String domainGuid) {
        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/domains?names=" + name + "&per_page=50",
                        getDomainJson(domainGuid));
    }

    private void stubEmptyDomainLookup(String name) {
        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/domains?names=" + name + "&per_page=50",
                        "{\"resources\":[]}");
    }

    private void stubRouteLookupWithResult() {
        String route = getRouteJson(ROUTE_GUID, "myhost", "/mypath", "myhost.example.com/mypath");
        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/routes?per_page=50&domain_guids="
                            + DOMAIN_GUID + "&space_guids=" + SPACE_GUID + "&hosts=myhost&paths=/mypath",
                        "{\"resources\":[" + route + "]}");
    }

    private void stubAppLookup(String appName) {
        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/apps?per_page=50"
                            + "&space_guids=" + SPACE_GUID + "&names=" + appName, "{\"resources\":[{\"guid\":\"" + APP_GUID + "\"}]}");
    }

    private void stubAsyncDelete(String uri) {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(uri))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.DELETE))
               .andRespond(MockRestResponseCreators.withStatus(HttpStatus.ACCEPTED)
                                                   .location(URI.create(MockControllerClientFactory.BASE_URL + "/v3/jobs/" + JOB_GUID)));
        factory.stubGet(MockControllerClientFactory.BASE_URL + "/v3/jobs/" + JOB_GUID,
                        "{\"guid\":\"" + JOB_GUID + "\",\"state\":\"COMPLETE\"}");
    }

    private static String getRouteJson(String guid, String host, String path, String url) {
        return "{\"guid\":\"" + guid + "\",\"host\":\"" + host + "\",\"path\":\"" + path + "\",\"url\":\"" + url + "\","
            + "\"destinations\":[],\"relationships\":{\"space\":{\"data\":{\"guid\":\"" + SPACE_GUID + "\"}},"
            + "\"domain\":{\"data\":{\"guid\":\"" + DOMAIN_GUID + "\"}}}}";
    }

    private static String getRouteWithDestinationJson() {
        return "{\"guid\":\"" + ROUTE_GUID + "\",\"host\":\"myhost\",\"path\":\"/mypath\",\"url\":\"myhost.example.com/mypath\","
            + "\"destinations\":[{\"guid\":\"" + DESTINATION_GUID + "\",\"app\":{\"guid\":\"" + APP_GUID + "\"},\"protocol\":\"http1\"}],"
            + "\"relationships\":{\"space\":{\"data\":{\"guid\":\"" + SPACE_GUID + "\"}},"
            + "\"domain\":{\"data\":{\"guid\":\"" + DOMAIN_GUID + "\"}}}}";
    }

    private static String getDomainJson(String domainGuid) {
        return "{\"resources\":[{\"guid\":\"" + domainGuid + "\",\"name\":\"" + DOMAIN_NAME + "\"}]}";
    }

}
