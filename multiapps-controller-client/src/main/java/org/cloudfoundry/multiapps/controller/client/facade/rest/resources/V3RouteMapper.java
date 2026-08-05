package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudRoute;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudDomain;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudMetadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudRoute;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableRouteDestination;
import org.cloudfoundry.multiapps.controller.client.facade.domain.RouteDestination;

/**
 * Maps the {@link V3Route} wire model to the project's {@link CloudRoute} domain object. Mirrors the OSS {@code RawCloudRoute} adapter
 * field-for-field (including the domain-name derivation from the route URL and the requested-protocol lookup by application GUID), so
 * both client implementations yield identical domain objects.
 */
public final class V3RouteMapper {

    private V3RouteMapper() {
    }

    /**
     * Maps a route without an application context (equivalent to OSS {@code ImmutableRawCloudRoute.of(route)}): the requested protocol is
     * always {@code null}.
     */
    public static CloudRoute toCloudRoute(V3Route route) {
        return toCloudRoute(route, null);
    }

    /**
     * Maps a route in the context of a specific application (equivalent to OSS
     * {@code ImmutableRawCloudRoute.builder().route(route).applicationGuid(applicationGuid).build()}): the requested protocol is taken
     * from the destination that points at {@code applicationGuid}.
     */
    public static CloudRoute toCloudRoute(V3Route route, UUID applicationGuid) {
        List<RouteDestination> destinations = mapDestinations(route);
        String domainGuid = route.relationships()
                                 .domain()
                                 .data()
                                 .guid();
        return ImmutableCloudRoute.builder()
                                  .metadata(V3ResourceMappers.parseMetadata(route.guid(), route.createdAt(), route.updatedAt()))
                                  .appsUsingRoute(route.destinations()
                                                       .size())
                                  .host(route.host())
                                  .port(route.port())
                                  .domain(ImmutableCloudDomain.builder()
                                                              .name(computeDomain(route))
                                                              .metadata(ImmutableCloudMetadata.of(UUID.fromString(domainGuid)))
                                                              .build())
                                  .path(route.path())
                                  .url(route.url())
                                  .destinations(destinations)
                                  .requestedProtocol(computeRequestedProtocol(destinations, applicationGuid))
                                  .build();
    }

    private static String computeDomain(V3Route route) {
        String domain = route.url();
        String host = route.host() == null ? "" : route.host();
        String path = route.path() == null ? "" : route.path();
        if (!host.isEmpty()) {
            domain = domain.substring(host.length() + 1);
        }
        if (!path.isEmpty()) {
            domain = domain.substring(0, domain.indexOf('/'));
        }
        if (route.port() != null) {
            domain = domain.substring(0, domain.indexOf(':'));
        }
        return domain;
    }

    private static List<RouteDestination> mapDestinations(V3Route route) {
        return route.destinations()
                    .stream()
                    .map(destination -> ImmutableRouteDestination.builder()
                                                                 .metadata(ImmutableCloudMetadata.builder()
                                                                                                 .guid(UUID.fromString(destination.guid()))
                                                                                                 .build())
                                                                 .applicationGuid(UUID.fromString(destination.app()
                                                                                                             .guid()))
                                                                 .port(destination.port())
                                                                 .weight(destination.weight())
                                                                 .protocol(destination.protocol())
                                                                 .build())
                    .collect(Collectors.toList());
    }

    private static String computeRequestedProtocol(List<RouteDestination> destinations, UUID applicationGuid) {
        if (applicationGuid == null) {
            return null;
        }
        return destinations.stream()
                           .filter(destination -> Objects.equals(destination.getApplicationGuid(), applicationGuid))
                           .findFirst()
                           .map(RouteDestination::getProtocol)
                           .orElse(null);
    }

}
