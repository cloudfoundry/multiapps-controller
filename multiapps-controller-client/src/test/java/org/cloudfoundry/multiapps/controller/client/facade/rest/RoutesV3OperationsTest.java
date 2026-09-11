package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudRoute;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudDomain;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudRoute;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;

class RoutesV3OperationsTest {

    private static final String SPACE_GUID = "11111111-1111-1111-1111-111111111111";
    private static final String DOMAIN_GUID = "22222222-2222-2222-2222-222222222222";
    private static final String ROUTE_GUID = "33333333-3333-3333-3333-333333333333";
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
        Mockito.when(target.getGuid())
               .thenReturn(UUID.fromString(SPACE_GUID));

        stubDomainLookup(DOMAIN_NAME, DOMAIN_GUID);
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/routes"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andRespond(MockRestResponseCreators.withSuccess(getRouteJson(), MediaType.APPLICATION_JSON));

        operations.addRoute("myhost", DOMAIN_NAME, "/mypath");

        factory.verify();
    }

    @Test
    void testAddRouteThrowsWhenDomainNotFound() {
        Mockito.when(target.getGuid())
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
        Mockito.when(target.getGuid())
               .thenReturn(UUID.fromString(SPACE_GUID));

        stubDomainLookup(DOMAIN_NAME, DOMAIN_GUID);
        stubRouteLookupWithResult();
        stubAsyncDelete(MockControllerClientFactory.BASE_URL + "/v3/routes/" + ROUTE_GUID);

        operations.deleteRoute("myhost", DOMAIN_NAME, "/mypath");

        factory.verify();
    }

    @Test
    void testDeleteRouteThrowsWhenRouteNotFound() {
        Mockito.when(target.getGuid())
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
        Mockito.when(target.getGuid())
               .thenReturn(UUID.fromString(SPACE_GUID));

        stubAsyncDelete(MockControllerClientFactory.BASE_URL + "/v3/spaces/" + SPACE_GUID + "/routes?unmapped=true");

        operations.deleteOrphanedRoutes();

        factory.verify();
    }

    @Test
    void testGetRoutesReturnsMappedRoutes() {
        Mockito.when(target.getGuid())
               .thenReturn(UUID.fromString(SPACE_GUID));

        stubDomainLookup(DOMAIN_NAME, DOMAIN_GUID);
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/routes?per_page=5000&domain_guids="
                                                             + DOMAIN_GUID + "&space_guids=" + SPACE_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[" + getRouteJson() + "]}", MediaType.APPLICATION_JSON));

        List<CloudRoute> routes = operations.getRoutes(DOMAIN_NAME);

        Assertions.assertEquals(1, routes.size());
        Assertions.assertEquals("myhost", routes.getFirst()
                                                .getHost());
        factory.verify();
    }

    @Test
    void testGetRoutesThrowsWhenDomainNotFound() {
        Mockito.when(target.getGuid())
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
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(
                   MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/routes?per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[" + getRouteJson() + "]}", MediaType.APPLICATION_JSON));

        List<CloudRoute> routes = operations.getApplicationRoutes(UUID.fromString(APP_GUID));

        Assertions.assertEquals(1, routes.size());
        Assertions.assertEquals("myhost.example.com/mypath", routes.getFirst()
                                                                   .getUrl());
        factory.verify();
    }

    @Test
    void testGetApplicationRoutesReturnsEmptyList() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(
                   MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/routes?per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[]}", MediaType.APPLICATION_JSON));

        List<CloudRoute> routes = operations.getApplicationRoutes(UUID.fromString(APP_GUID));

        Assertions.assertEquals(0, routes.size());
        factory.verify();
    }

    @Test
    void testUpdateApplicationRoutesAddsNewRoute() {
        Mockito.when(target.getGuid())
               .thenReturn(UUID.fromString(SPACE_GUID));

        stubAppLookup("my-app");

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(
                   MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/routes?per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[]}", MediaType.APPLICATION_JSON));

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(
                   MockControllerClientFactory.BASE_URL + "/v3/domains?names=" + DOMAIN_NAME + "&per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(getDomainJson(DOMAIN_GUID), MediaType.APPLICATION_JSON));

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/routes?per_page=5000&domain_guids="
                                                             + DOMAIN_GUID + "&space_guids=" + SPACE_GUID + "&hosts=myhost&paths=/mypath"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[]}", MediaType.APPLICATION_JSON));

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/routes"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andRespond(MockRestResponseCreators.withSuccess(getRouteJson(), MediaType.APPLICATION_JSON));

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(
                   MockControllerClientFactory.BASE_URL + "/v3/routes/" + ROUTE_GUID + "/destinations"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
               .andRespond(MockRestResponseCreators.withSuccess());

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
        Mockito.when(target.getGuid())
               .thenReturn(UUID.fromString(SPACE_GUID));

        stubAppLookup("my-app");

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(
                   MockControllerClientFactory.BASE_URL + "/v3/apps/" + APP_GUID + "/routes?per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[" + getRouteWithDestinationJson() + "]}",
                                                                MediaType.APPLICATION_JSON));

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(
                   MockControllerClientFactory.BASE_URL + "/v3/routes/" + ROUTE_GUID + "/destinations/" + DESTINATION_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.DELETE))
               .andRespond(MockRestResponseCreators.withSuccess());

        operations.updateApplicationRoutes("my-app", Set.of());

        factory.verify();
    }

    @Test
    void testUpdateApplicationRoutesThrowsWhenApplicationNotFound() {
        Mockito.when(target.getGuid())
               .thenReturn(UUID.fromString(SPACE_GUID));

        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps?per_page=5000&names=missing-app"
                                                             + "&space_guids=" + SPACE_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[]}", MediaType.APPLICATION_JSON));

        Assertions.assertThrows(CloudOperationException.class, () -> operations.updateApplicationRoutes("missing-app", Set.of()));
    }

    private void stubDomainLookup(String name, String domainGuid) {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(
                   MockControllerClientFactory.BASE_URL + "/v3/domains?names=" + name + "&per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess(getDomainJson(domainGuid), MediaType.APPLICATION_JSON));
    }

    private void stubEmptyDomainLookup(String name) {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(
                   MockControllerClientFactory.BASE_URL + "/v3/domains?names=" + name + "&per_page=5000"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[]}", MediaType.APPLICATION_JSON));
    }

    private void stubRouteLookupWithResult() {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/routes?per_page=5000&domain_guids="
                                                             + DOMAIN_GUID + "&space_guids=" + SPACE_GUID + "&hosts=myhost&paths=/mypath"))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[" + getRouteJson() + "]}", MediaType.APPLICATION_JSON));
    }

    private void stubAppLookup(String appName) {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/apps?per_page=5000&names=" + appName
                                                             + "&space_guids=" + SPACE_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"resources\":[{\"guid\":\"" + APP_GUID + "\"}]}",
                                                                MediaType.APPLICATION_JSON));
    }

    private void stubAsyncDelete(String uri) {
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(uri))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.DELETE))
               .andRespond(MockRestResponseCreators.withStatus(HttpStatus.ACCEPTED)
                                                   .location(URI.create(MockControllerClientFactory.BASE_URL + "/v3/jobs/" + JOB_GUID)));
        factory.server()
               .expect(MockRestRequestMatchers.requestTo(MockControllerClientFactory.BASE_URL + "/v3/jobs/" + JOB_GUID))
               .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
               .andRespond(MockRestResponseCreators.withSuccess("{\"guid\":\"" + JOB_GUID + "\",\"state\":\"COMPLETE\"}",
                                                                MediaType.APPLICATION_JSON));
    }

    private static String getRouteJson() {
        return "{\"guid\":\"" + ROUTE_GUID + "\",\"host\":\"myhost\",\"path\":\"/mypath\",\"url\":\"myhost.example.com/mypath\","
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
