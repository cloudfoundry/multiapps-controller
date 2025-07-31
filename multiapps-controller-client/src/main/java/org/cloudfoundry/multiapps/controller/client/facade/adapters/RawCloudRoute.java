package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.cloudfoundry.client.v3.routes.Route;
import org.immutables.value.Value;

import org.cloudfoundry.multiapps.controller.client.facade.Nullable;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudRoute;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudDomain;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudMetadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudRoute;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableRouteDestination;
import org.cloudfoundry.multiapps.controller.client.facade.domain.RouteDestination;

@Value.Immutable
public abstract class RawCloudRoute extends RawCloudEntity<CloudRoute> {

    @Value.Parameter
    public abstract Route getRoute();

    @Nullable
    public abstract UUID getApplicationGuid();

    @Override
    public CloudRoute derive() {
        Route route = getRoute();
        List<RouteDestination> destinations = mapDestinations();
        String domainGuid = route.getRelationships()
                                 .getDomain()
                                 .getData()
                                 .getId();
        return ImmutableCloudRoute.builder()
                                  .metadata(parseResourceMetadata(route))
                                  .appsUsingRoute(route.getDestinations()
                                                       .size())
                                  .host(route.getHost())
                                  .port(route.getPort())
                                  .domain(ImmutableCloudDomain.builder()
                                                              .name(computeDomain(route))
                                                              .metadata(ImmutableCloudMetadata.of(UUID.fromString(domainGuid)))
                                                              .build())
                                  .path(route.getPath())
                                  .url(route.getUrl())
                                  .destinations(destinations)
                                  .requestedProtocol(computeRequestedProtocol(destinations))
                                  .build();
    }

    private static String computeDomain(Route route) {
        String domain = route.getUrl();
        if (!route.getHost()
                  .isEmpty()) {
            domain = domain.substring(route.getHost()
                                           .length()
                + 1);
        }
        if (!route.getPath()
                  .isEmpty()) {
            domain = domain.substring(0, domain.indexOf('/'));
        }
        if (route.getPort() != null) {
            domain = domain.substring(0, domain.indexOf(':'));
        }
        return domain;
    }

    private List<RouteDestination> mapDestinations() {
        return getRoute().getDestinations()
                         .stream()
                         .map(destination -> ImmutableRouteDestination.builder()
                                                                      .metadata(ImmutableCloudMetadata.builder()
                                                                                                      .guid(UUID.fromString(destination.getDestinationId()))
                                                                                                      .build())
                                                                      .applicationGuid(UUID.fromString(destination.getApplication()
                                                                                                                  .getApplicationId()))
                                                                      .port(destination.getPort())
                                                                      .weight(destination.getWeight())
                                                                      .protocol(destination.getProtocol())
                                                                      .build())
                         .collect(Collectors.toList());
    }

    private String computeRequestedProtocol(List<RouteDestination> destinations) {
        UUID applicationGuid = getApplicationGuid();
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
