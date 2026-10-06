package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.Constants;
import org.cloudfoundry.multiapps.controller.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudRoute;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.RouteDestination;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Domain;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Fields;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3GuidReference;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Route;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3RouteMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public class RoutesV3Operations {

    private static final ParameterizedTypeReference<V3ListResponse<V3Route>> ROUTE_PAGE = new ParameterizedTypeReference<>() {
    };

    private static final ParameterizedTypeReference<V3ListResponse<V3Domain>> DOMAIN_PAGE = new ParameterizedTypeReference<>() {
    };

    private static final ParameterizedTypeReference<V3ListResponse<V3GuidReference>> APP_PAGE = new ParameterizedTypeReference<>() {
    };

    private final CloudControllerV3Client client;

    private final CloudSpace targetSpace;

    public RoutesV3Operations(CloudControllerV3Client client, CloudSpace targetSpace) {
        this.client = client;
        this.targetSpace = targetSpace;
    }

    public void addRoute(String host, String domainName, String path) {
        assertSpaceProvided("add route for domain");
        UUID domainGuid = getRequiredDomainGuid(domainName);
        addRoute(domainGuid, host, path);
    }

    public void deleteRoute(String host, String domainName, String path) {
        assertSpaceProvided("delete route for domain");

        UUID routeGuid = getRouteGuid(getRequiredDomainGuid(domainName), host, path);
        if (routeGuid == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                              MessageFormat.format(Messages.HOST_0_NOT_FOUND_FOR_DOMAIN_1, host, domainName));
        }

        deleteRoute(routeGuid);
    }

    public void deleteOrphanedRoutes() {
        ResponseEntity<Void> response = client.delete(CloudControllerV3Endpoints.SPACE_UNMAPPED_ROUTES, getTargetSpaceGuid());

        client.followAsyncJob(response, Constants.DELETE_JOB_TIMEOUT);
    }

    public List<CloudRoute> getRoutes(String domainName) {
        assertSpaceProvided("get routes for domain");
        UUID domainGuid = getRequiredDomainGuid(domainName);

        return findRoutesByDomainGuid(domainGuid);
    }

    public List<CloudRoute> getApplicationRoutes(UUID applicationGuid) {
        return listRoutes(CloudControllerV3Endpoints.APPS + "/" + applicationGuid + "/routes" + CloudControllerV3Endpoints.QUERY_PER_PAGE
                              + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE).stream()
                                                                             .map(route -> V3RouteMapper.toCloudRoute(
                                                                                 route, applicationGuid))
                                                                             .toList();
    }

    public void updateApplicationRoutes(String applicationName, Set<CloudRoute> updatedRoutes) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);

        List<CloudRoute> appRoutes = listRoutes(
            CloudControllerV3Endpoints.APPS + "/" + applicationGuid + "/routes" + CloudControllerV3Endpoints.QUERY_PER_PAGE
                + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE).stream()
                                                               .map(V3RouteMapper::toCloudRoute)
                                                               .toList();

        Set<CloudRoute> outdatedRoutes = OperationsUtil.getOutdatedRoutes(applicationGuid, appRoutes, updatedRoutes);
        Set<CloudRoute> newRoutes = OperationsUtil.getNewRoutes(applicationGuid, appRoutes, updatedRoutes);

        removeRoutes(outdatedRoutes, applicationGuid);
        addRoutes(newRoutes, applicationGuid);
    }

    private List<CloudRoute> findRoutesByDomainGuid(UUID domainGuid) {
        UUID targetSpaceGuid = getTargetSpaceGuid();
        String routeQuery = OperationsQueryUtil.buildRoutesQuery(domainGuid, null, null, targetSpaceGuid);

        return listRoutes(routeQuery).stream()
                                     .map(V3RouteMapper::toCloudRoute)
                                     .toList();
    }

    private UUID getRouteGuid(UUID domainGuid, String host, String path) {
        UUID targetSpaceGuid = getTargetSpaceGuid();
        String routeQuery = OperationsQueryUtil.buildRoutesQuery(domainGuid, host, path, targetSpaceGuid);

        List<V3Route> routes = listRoutes(routeQuery);
        if (CollectionUtils.isEmpty(routes)) {
            return null;
        }

        return UUID.fromString(routes.getFirst()
                                     .guid());
    }

    private List<V3Route> listRoutes(String query) {
        return client.list(query, ROUTE_PAGE);
    }

    private void addRoutes(Set<CloudRoute> routes, UUID applicationGuid) {
        Map<String, UUID> domains = getDomainsFromRoutes(routes);

        for (CloudRoute route : routes) {
            OperationsUtil.validateDomainForRoute(route, domains);

            UUID domainGuid = domains.get(route.getDomain()
                                               .getName());
            UUID routeGuid = getOrAddRoute(domainGuid, route.getHost(), route.getPath());

            bindRoute(routeGuid, applicationGuid, route.getRequestedProtocol());
        }
    }

    private void removeRoutes(Set<CloudRoute> routes, UUID applicationGuid) {
        for (CloudRoute route : routes) {
            unbindRouteFromDestinationApplications(route, applicationGuid);
        }
    }

    private void unbindRouteFromDestinationApplications(CloudRoute route, UUID applicationGuid) {
        List<RouteDestination> destinationsForCurrentRoute = route.getDestinations();

        for (RouteDestination destination : destinationsForCurrentRoute) {
            if (destination.getApplicationGuid()
                           .equals(applicationGuid)) {
                unbindRoute(route.getGuid(), destination.getGuid());
            }
        }
    }

    private UUID getOrAddRoute(UUID domainGuid, String host, String path) {
        UUID routeGuid = getRouteGuid(domainGuid, host, path);

        if (routeGuid == null) {
            routeGuid = addRoute(domainGuid, host, path);
        }

        return routeGuid;
    }

    private UUID addRoute(UUID domainGuid, String host, String path) {
        assertSpaceProvided("add route");
        UUID targetSpaceGuid = getTargetSpaceGuid();
        Map<String, Object> createRouteBody = OperationsUtil.buildCreateRouteBody(domainGuid, host, path, targetSpaceGuid);

        V3Route created = client.postForObject(CloudControllerV3Endpoints.ROUTES, createRouteBody, V3Route.class);

        return UUID.fromString(created.guid());
    }

    private void deleteRoute(UUID guid) {
        ResponseEntity<Void> response = client.delete(CloudControllerV3Endpoints.ROUTE_BY_GUID, guid);

        client.followAsyncJob(response, Constants.DELETE_JOB_TIMEOUT);
    }

    private void bindRoute(UUID routeGuid, UUID applicationGuid, String protocol) {
        client.post(CloudControllerV3Endpoints.ROUTE_DESTINATIONS,
                    Map.of(V3Fields.DESTINATIONS, List.of(OperationsUtil.createDestination(applicationGuid, protocol))), routeGuid);
    }

    private void unbindRoute(UUID routeGuid, UUID destinationGuid) {
        client.delete(CloudControllerV3Endpoints.ROUTE_DESTINATION_BY_GUID, routeGuid, destinationGuid);
    }

    private UUID getRequiredDomainGuid(String name) {
        UUID guid = findDomainGuidByName(name);

        if (guid == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                              MessageFormat.format(Messages.DOMAIN_0_NOT_FOUND, name));
        }

        return guid;
    }

    private UUID findDomainGuidByName(String name) {
        List<V3Domain> domains = client.list(CloudControllerV3Endpoints.DOMAINS + CloudControllerV3Endpoints.QUERY_NAMES + name
                                                 + CloudControllerV3Endpoints.AMPERSAND_PER_PAGE
                                                 + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE,
                                             DOMAIN_PAGE);

        if (CollectionUtils.isEmpty(domains)) {
            return null;
        }

        return UUID.fromString(domains.getFirst()
                                      .guid());
    }

    private Map<String, UUID> getDomainsFromRoutes(Set<CloudRoute> routes) {
        Set<String> domainNames = routes.stream()
                                        .map(route -> route.getDomain()
                                                           .getName())
                                        .filter(StringUtils::hasLength)
                                        .collect(Collectors.toSet());

        if (domainNames.isEmpty()) {
            return Map.of();
        }

        String names = String.join(",", domainNames);
        return client.list(CloudControllerV3Endpoints.DOMAINS + CloudControllerV3Endpoints.QUERY_NAMES + names
                               + CloudControllerV3Endpoints.AMPERSAND_PER_PAGE + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE, DOMAIN_PAGE)
                     .stream()
                     .collect(Collectors.toMap(V3Domain::name, domain -> UUID.fromString(domain.guid())));
    }

    private UUID getRequiredApplicationGuid(String applicationName) {
        String applicationsQuery = OperationsQueryUtil.buildApplicationsQuery(applicationName, targetSpace);
        List<V3GuidReference> apps = client.list(applicationsQuery,
                                                 APP_PAGE);

        if (CollectionUtils.isEmpty(apps)) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                              MessageFormat.format(Messages.APPLICATION_0_NOT_FOUND, applicationName));
        }

        return UUID.fromString(apps.getFirst()
                                   .guid());
    }

    private UUID getTargetSpaceGuid() {
        return targetSpace.getGuid();
    }

    private void assertSpaceProvided(String operation) {
        if (targetSpace == null) {
            throw new IllegalArgumentException(
                MessageFormat.format(Messages.UNABLE_TO_0_WITHOUT_SPECIFYING_ORGANIZATION_AND_SPACE_TO_USE, operation));
        }
    }

}
