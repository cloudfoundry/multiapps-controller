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
import org.springframework.util.StringUtils;

public final class V3RouteMapper {

    private V3RouteMapper() {
    }

    public static CloudRoute toCloudRoute(V3Route route) {
        return toCloudRoute(route, null);
    }

    public static CloudRoute toCloudRoute(V3Route route, UUID applicationGuid) {
        List<RouteDestination> destinations = mapDestinations(route);
        String domainGuid = extractDomainGuid(route);

        return ImmutableCloudRoute.builder()
                                  .metadata(V3ResourceMappers.parseMetadata(route.guid(), route.createdAt(), route.updatedAt()))
                                  .appsUsingRoute(route.destinations()
                                                       .size())
                                  .host(route.host())
                                  .port(route.port())
                                  .domain(ImmutableCloudDomain.builder()
                                                              .name(computeDomain(route))
                                                              .metadata(ImmutableCloudMetadata.of(
                                                                  V3ResourceMappers.parseNullableGuid(domainGuid)))
                                                              .build())
                                  .path(route.path())
                                  .url(route.url())
                                  .destinations(destinations)
                                  .requestedProtocol(computeRequestedProtocol(destinations, applicationGuid))
                                  .build();
    }

    private static String extractDomainGuid(V3Route route) {
        if (route.relationships() == null || route.relationships()
                                                  .domain() == null
            || route.relationships()
                    .domain()
                    .data() == null) {
            return null;
        }

        return route.relationships()
                    .domain()
                    .data()
                    .guid();
    }

    private static String computeDomain(V3Route route) {
        String domain = route.url();

        if (StringUtils.hasText(route.host())) {
            domain = domain.substring(route.host()
                                           .length() + 1);
        }

        if (StringUtils.hasText(route.path())) {
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
                    .filter(destination -> destination.app() != null && destination.app()
                                                                                   .guid() != null)
                    .map(destination -> ImmutableRouteDestination.builder()
                                                                 .metadata(ImmutableCloudMetadata.builder()
                                                                                                 .guid(V3ResourceMappers.parseNullableGuid(
                                                                                                     destination.guid()))
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
