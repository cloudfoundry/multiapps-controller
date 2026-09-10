package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudRoute;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Route.V3Destination;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Route.V3DestinationApp;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Route.V3RelationshipData;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Route.V3RouteRelationships;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Route.V3ToOneRelationship;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class V3RouteMapperTest {

    private static final String ROUTE_GUID = "3725a721-6e3f-4b6e-8f2b-1c2d3e4f5a6b";
    private static final String DOMAIN_GUID = "11111111-2222-3333-4444-555555555555";
    private static final String APP_GUID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final String DESTINATION_GUID = "99999999-8888-7777-6666-555555555555";

    @Test
    void testToCloudRouteComputesHostBasedDomain() {
        V3Route route = buildRoute("myhost", "", null, "myhost.example.com", List.of());

        CloudRoute result = V3RouteMapper.toCloudRoute(route);

        Assertions.assertEquals("example.com", result.getDomain()
                                                     .getName());
        Assertions.assertEquals("myhost", result.getHost());
    }

    @Test
    void testToCloudRouteComputesDomainWhenNoHost() {
        V3Route route = buildRoute("", "", null, "example.com", List.of());

        Assertions.assertEquals("example.com", V3RouteMapper.toCloudRoute(route)
                                                            .getDomain()
                                                            .getName());
    }

    @Test
    void testToCloudRouteStripsPathFromDomain() {
        V3Route route = buildRoute("myhost", "/foo", null, "myhost.example.com/foo", List.of());

        Assertions.assertEquals("example.com", V3RouteMapper.toCloudRoute(route)
                                                            .getDomain()
                                                            .getName());
    }

    @Test
    void testToCloudRouteStripsPortFromDomain() {
        V3Route route = buildRoute("", "", 8080, "tcp.example.com:8080", List.of());

        Assertions.assertEquals("tcp.example.com", V3RouteMapper.toCloudRoute(route)
                                                                .getDomain()
                                                                .getName());
    }

    @Test
    void testToCloudRouteCountsAppsUsingRoute() {
        V3Route route = buildRoute("myhost", "", null, "myhost.example.com",
                                   List.of(buildDestination("http1"), buildDestination("http2")));

        Assertions.assertEquals(2, V3RouteMapper.toCloudRoute(route)
                                                .getAppsUsingRoute());
    }

    @Test
    void testToCloudRouteRequestedProtocolIsNullWhenNoApplicationGuidGiven() {
        V3Route route = buildRoute("myhost", "", null, "myhost.example.com", List.of(buildDestination("http2")));

        Assertions.assertNull(V3RouteMapper.toCloudRoute(route)
                                           .getRequestedProtocol());
    }

    @Test
    void testToCloudRouteRequestedProtocolMatchesApplicationGuid() {
        V3Route route = buildRoute("myhost", "", null, "myhost.example.com", List.of(buildDestination("http2")));

        CloudRoute result = V3RouteMapper.toCloudRoute(route, UUID.fromString(APP_GUID));

        Assertions.assertEquals("http2", result.getRequestedProtocol());
    }

    @Test
    void testToCloudRouteRequestedProtocolIsNullWhenApplicationGuidDoesNotMatch() {
        V3Route route = buildRoute("myhost", "", null, "myhost.example.com", List.of(buildDestination("http2")));

        CloudRoute result = V3RouteMapper.toCloudRoute(route, UUID.randomUUID());

        Assertions.assertNull(result.getRequestedProtocol());
    }

    private static V3Route buildRoute(String host, String path, Integer port, String url, List<V3Destination> destinations) {
        V3RouteRelationships relationships = new V3RouteRelationships(new V3ToOneRelationship(new V3RelationshipData(DOMAIN_GUID)),
                                                                      new V3ToOneRelationship(new V3RelationshipData(DOMAIN_GUID)));
        return new V3Route(ROUTE_GUID, host, path, port, url, null, null, destinations, null, relationships);
    }

    private static V3Destination buildDestination(String protocol) {
        return new V3Destination(DESTINATION_GUID, new V3DestinationApp(APP_GUID), 8080, 1, protocol);
    }

}
