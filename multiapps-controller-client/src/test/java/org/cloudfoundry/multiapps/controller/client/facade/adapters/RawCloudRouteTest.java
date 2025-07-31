package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import java.util.List;
import java.util.UUID;

import org.cloudfoundry.client.v3.Relationship;
import org.cloudfoundry.client.v3.ToOneRelationship;
import org.cloudfoundry.client.v3.routes.Application;
import org.cloudfoundry.client.v3.routes.Destination;
import org.cloudfoundry.client.v3.routes.Route;
import org.cloudfoundry.client.v3.routes.RouteRelationships;
import org.cloudfoundry.client.v3.routes.RouteResource;
import org.junit.jupiter.api.Test;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudDomain;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudRoute;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudDomain;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudRoute;

class RawCloudRouteTest {

    private static final String HOST = "foo";
    private static final String DOMAIN_NAME = "example.com";
    private static final CloudDomain DOMAIN = buildTestDomain();
    private static final Integer APPS_USING_ROUTE = 2;
    private static final UUID DESTINATION_1_GUID = UUID.randomUUID();
    private static final UUID APPLICATION_1_GUID = UUID.randomUUID();
    private static final String DESTINATION_1_PROTOCOL = "http2";
    private static final UUID DESTINATION_2_GUID = UUID.randomUUID();
    private static final UUID APPLICATION_2_GUID = UUID.randomUUID();
    private static final String DESTINATION_2_PROTOCOL = "http1";
    private static final List<Destination> DESTINATIONS = buildTestDestinations();

    @Test
    void testDerive() {
        RawCloudEntityTest.testDerive(buildExpectedRoute(), buildRawRoute());
    }

    private static CloudRoute buildExpectedRoute() {
        return ImmutableCloudRoute.builder()
                                  .metadata(RawCloudEntityTest.EXPECTED_METADATA_V3)
                                  .host(HOST)
                                  .domain(DOMAIN)
                                  .path("")
                                  .appsUsingRoute(APPS_USING_ROUTE)
                                  .url(HOST + "." + DOMAIN_NAME)
                                  .build();
    }

    private static RawCloudRoute buildRawRoute() {
        return ImmutableRawCloudRoute.builder()
                                     .route(buildTestRoute())
                                     .build();
    }

    private static Route buildTestRoute() {
        return RouteResource.builder()
                            .id(RawCloudEntityTest.GUID_STRING)
                            .createdAt(RawCloudEntityTest.CREATED_AT_STRING)
                            .updatedAt(RawCloudEntityTest.UPDATED_AT_STRING)
                            .relationships(RouteRelationships.builder()
                                                             .space(buildToOneRelationship(RawCloudEntityTest.GUID))
                                                             .domain(buildToOneRelationship(RawCloudEntityTest.GUID))
                                                             .build())
                            .metadata(RawCloudEntityTest.V3_METADATA)
                            .host(HOST)
                            .path("")
                            .url(HOST + "." + DOMAIN_NAME)
                            .addAllDestinations(DESTINATIONS)
                            .build();
    }

    private static CloudDomain buildTestDomain() {
        return ImmutableCloudDomain.builder()
                                   .metadata(RawCloudEntityTest.EXPECTED_METADATA_V3)
                                   .name(DOMAIN_NAME)
                                   .build();
    }

    private static List<Destination> buildTestDestinations() {
        return List.of(buildDestination(DESTINATION_1_GUID.toString(), APPLICATION_1_GUID.toString(), DESTINATION_1_PROTOCOL),
                       buildDestination(DESTINATION_2_GUID.toString(), APPLICATION_2_GUID.toString(), DESTINATION_2_PROTOCOL));
    }

    private static Destination buildDestination(String destinationGuid, String applicationGuid, String protocol) {
        return Destination.builder()
                          .destinationId(destinationGuid)
                          .application(Application.builder()
                                                  .applicationId(applicationGuid)
                                                  .build())
                          .protocol(protocol)
                          .build();
    }

    private static ToOneRelationship buildToOneRelationship(UUID guid) {
        return ToOneRelationship.builder()
                                .data(buildRelationship(guid))
                                .build();
    }

    private static Relationship buildRelationship(UUID guid) {
        return Relationship.builder()
                           .id(guid.toString())
                           .build();
    }

}
