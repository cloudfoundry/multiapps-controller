package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudRoute;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.RouteDestination;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Domain;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Route;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3RouteMapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * CF v3 <em>routes</em> operations of the cf-java-client replacement. Reproduces the HTTP shape, async handling and domain mapping of the
 * OSS {@code CloudControllerRestClientImpl} route methods:
 * <ul>
 * <li>{@code addRoute(host, domainName, path)} &rarr; resolve the domain, then {@code POST /v3/routes} (idempotent: reuse an existing
 * route if one already matches);</li>
 * <li>{@code deleteRoute(host, domainName, path)} &rarr; resolve the route in the target space and {@code DELETE /v3/routes/{guid}}
 * (async job), 404 if not found;</li>
 * <li>{@code deleteOrphanedRoutes()} &rarr; {@code DELETE /v3/spaces/{guid}/routes?unmapped=true} (async job);</li>
 * <li>{@code getRoutes(domainName)} &rarr; {@code GET /v3/routes?domain_guids=…&space_guids=…} (paginated);</li>
 * <li>{@code getApplicationRoutes(appGuid)} &rarr; {@code GET /v3/apps/{guid}/routes} (paginated);</li>
 * <li>{@code updateApplicationRoutes(appName, routes)} &rarr; diff current vs requested destinations, then insert/remove destinations.</li>
 * </ul>
 * All listings are scoped to the target space exactly where the OSS impl scopes them.
 */
public class RoutesV3Operations {

    private static final ParameterizedTypeReference<V3ListResponse<V3Route>> ROUTE_PAGE = new ParameterizedTypeReference<>() {
    };
    private static final ParameterizedTypeReference<V3ListResponse<V3Domain>> DOMAIN_PAGE = new ParameterizedTypeReference<>() {
    };

    // CF v3 caps per_page at 5000; matches the other V3 operations.
    private static final int DEFAULT_PAGE_SIZE = 5000;
    // Mirrors the OSS DELETE_JOB_TIMEOUT.
    private static final Duration DELETE_JOB_TIMEOUT = Duration.ofMinutes(5);

    private final CloudControllerV3Client cc;
    private final CloudSpace target;

    public RoutesV3Operations(CloudControllerV3Client cc, CloudSpace target) {
        this.cc = cc;
        this.target = target;
    }

    public void addRoute(String host, String domainName, String path) {
        assertSpaceProvided("add route for domain");
        UUID domainGuid = getRequiredDomainGuid(domainName);
        doAddRoute(domainGuid, host, path);
    }

    public void deleteRoute(String host, String domainName, String path) {
        assertSpaceProvided("delete route for domain");
        UUID routeGuid = getRouteGuid(getRequiredDomainGuid(domainName), host, path);
        if (routeGuid == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found",
                                              "Host " + host + " not found for domain " + domainName + ".");
        }
        doDeleteRoute(routeGuid);
    }

    public void deleteOrphanedRoutes() {
        ResponseEntity<Void> response = cc.getRestClient()
                                          .delete()
                                          .uri("/v3/spaces/{guid}/routes?unmapped=true", getTargetSpaceGuid())
                                          .retrieve()
                                          .toEntity(Void.class);
        cc.followAsyncJob(response, DELETE_JOB_TIMEOUT);
    }

    public List<CloudRoute> getRoutes(String domainName) {
        assertSpaceProvided("get routes for domain");
        UUID domainGuid = getRequiredDomainGuid(domainName);
        return findRoutesByDomainGuid(domainGuid);
    }

    public List<CloudRoute> getApplicationRoutes(UUID applicationGuid) {
        return listRoutes("/v3/apps/" + applicationGuid + "/routes?per_page=" + DEFAULT_PAGE_SIZE).stream()
                                                                                                  .map(route -> V3RouteMapper.toCloudRoute(
                                                                                                      route, applicationGuid))
                                                                                                  .toList();
    }

    public void updateApplicationRoutes(String applicationName, Set<CloudRoute> updatedRoutes) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        List<CloudRoute> appRoutes = listRoutes(
            "/v3/apps/" + applicationGuid + "/routes?per_page=" + DEFAULT_PAGE_SIZE).stream()
                                                                                   .map(V3RouteMapper::toCloudRoute)
                                                                                   .toList();
        Set<CloudRoute> outdatedRoutes = getOutdatedRoutes(applicationGuid, appRoutes, updatedRoutes);
        Set<CloudRoute> newRoutes = getNewRoutes(applicationGuid, appRoutes, updatedRoutes);
        removeRoutes(outdatedRoutes, applicationGuid);
        addRoutes(newRoutes, applicationGuid);
    }

    // --- route listing helpers -------------------------------------------------------------------

    private List<CloudRoute> findRoutesByDomainGuid(UUID domainGuid) {
        StringBuilder query = new StringBuilder("/v3/routes?per_page=" + DEFAULT_PAGE_SIZE);
        query.append("&domain_guids=")
             .append(domainGuid);
        query.append("&space_guids=")
             .append(getTargetSpaceGuid());
        return listRoutes(query.toString()).stream()
                                           .map(V3RouteMapper::toCloudRoute)
                                           .toList();
    }

    private UUID getRouteGuid(UUID domainGuid, String host, String path) {
        StringBuilder query = new StringBuilder("/v3/routes?per_page=" + DEFAULT_PAGE_SIZE);
        query.append("&domain_guids=")
             .append(domainGuid);
        query.append("&space_guids=")
             .append(getTargetSpaceGuid());
        if (host != null) {
            query.append("&hosts=")
                 .append(host);
        }
        if (path != null) {
            query.append("&paths=")
                 .append(path);
        }
        List<V3Route> routes = listRoutes(query.toString());
        if (CollectionUtils.isEmpty(routes)) {
            return null;
        }
        return UUID.fromString(routes.get(0)
                                     .guid());
    }

    private List<V3Route> listRoutes(String query) {
        return cc.list(query, ROUTE_PAGE);
    }

    // --- add / bind / remove ---------------------------------------------------------------------

    private void addRoutes(Set<CloudRoute> routes, UUID applicationGuid) {
        Map<String, UUID> domains = getDomainsFromRoutes(routes);
        for (CloudRoute route : routes) {
            validateDomainForRoute(route, domains);
            UUID domainGuid = domains.get(route.getDomain()
                                               .getName());
            UUID routeGuid = getOrAddRoute(domainGuid, route.getHost(), route.getPath());
            bindRoute(routeGuid, applicationGuid, route.getRequestedProtocol());
        }
    }

    private void removeRoutes(Set<CloudRoute> routes, UUID applicationGuid) {
        for (CloudRoute route : routes) {
            for (RouteDestination destination : route.getDestinations()) {
                if (destination.getApplicationGuid()
                               .equals(applicationGuid)) {
                    unbindRoute(route.getGuid(), destination.getGuid());
                }
            }
        }
    }

    private UUID getOrAddRoute(UUID domainGuid, String host, String path) {
        UUID routeGuid = getRouteGuid(domainGuid, host, path);
        if (routeGuid == null) {
            routeGuid = doAddRoute(domainGuid, host, path);
        }
        return routeGuid;
    }

    private UUID doAddRoute(UUID domainGuid, String host, String path) {
        assertSpaceProvided("add route");
        V3Route created = cc.getRestClient()
                            .post()
                            .uri("/v3/routes")
                            .body(Map.of("host", host == null ? "" : host, "path", path == null ? "" : path, "relationships",
                                         Map.of("domain", toOneRelationship(domainGuid), "space",
                                                toOneRelationship(getTargetSpaceGuid()))))
                            .retrieve()
                            .body(V3Route.class);
        return UUID.fromString(created.guid());
    }

    private void doDeleteRoute(UUID guid) {
        ResponseEntity<Void> response = cc.getRestClient()
                                          .delete()
                                          .uri("/v3/routes/{guid}", guid)
                                          .retrieve()
                                          .toEntity(Void.class);
        cc.followAsyncJob(response, DELETE_JOB_TIMEOUT);
    }

    private void bindRoute(UUID routeGuid, UUID applicationGuid, String protocol) {
        cc.getRestClient()
          .post()
          .uri("/v3/routes/{guid}/destinations", routeGuid)
          .body(Map.of("destinations", List.of(createDestination(applicationGuid, protocol))))
          .retrieve()
          .toBodilessEntity();
    }

    private void unbindRoute(UUID routeGuid, UUID destinationGuid) {
        cc.getRestClient()
          .delete()
          .uri("/v3/routes/{routeGuid}/destinations/{destinationGuid}", routeGuid, destinationGuid)
          .retrieve()
          .toBodilessEntity();
    }

    private Map<String, Object> createDestination(UUID applicationGuid, String protocol) {
        if (protocol == null) {
            return Map.of("app", Map.of("guid", applicationGuid.toString()));
        }
        return Map.of("app", Map.of("guid", applicationGuid.toString()), "protocol", protocol);
    }

    private static Map<String, Object> toOneRelationship(UUID guid) {
        return Map.of("data", Map.of("guid", guid.toString()));
    }

    // --- diffing (mirror of the OSS impl) --------------------------------------------------------

    private Set<CloudRoute> getOutdatedRoutes(UUID applicationGuid, List<CloudRoute> currentRoutes, Set<CloudRoute> updatedRoutes) {
        return currentRoutes.stream()
                            .filter(currentRoute -> isRouteOutdated(applicationGuid, currentRoute, updatedRoutes))
                            .collect(Collectors.toSet());
    }

    private boolean isRouteOutdated(UUID applicationGuid, CloudRoute currentRoute, Set<CloudRoute> updatedRoutes) {
        Optional<CloudRoute> updatedRoute = findRoute(currentRoute.getUrl(), updatedRoutes);
        if (updatedRoute.isEmpty()) {
            return true;
        }
        return isProtocolChanged(applicationGuid, currentRoute, updatedRoute.get());
    }

    private Set<CloudRoute> getNewRoutes(UUID applicationGuid, List<CloudRoute> currentRoutes, Set<CloudRoute> updatedRoutes) {
        return updatedRoutes.stream()
                            .filter(updatedRoute -> isRouteUpdated(applicationGuid, updatedRoute, currentRoutes))
                            .collect(Collectors.toSet());
    }

    private boolean isRouteUpdated(UUID applicationGuid, CloudRoute updatedRoute, List<CloudRoute> currentRoutes) {
        Optional<CloudRoute> currentRoute = findRoute(updatedRoute.getUrl(), currentRoutes);
        if (currentRoute.isEmpty()) {
            return true;
        }
        return isProtocolChanged(applicationGuid, currentRoute.get(), updatedRoute);
    }

    private Optional<CloudRoute> findRoute(String url, java.util.Collection<CloudRoute> routes) {
        return routes.stream()
                     .filter(route -> Objects.equals(url, route.getUrl()))
                     .findFirst();
    }

    private boolean isProtocolChanged(UUID applicationGuid, CloudRoute currentRoute, CloudRoute updatedRoute) {
        if (updatedRoute.getRequestedProtocol() == null) {
            return false;
        }
        return currentRoute.getDestinations()
                           .stream()
                           .filter(routeDestination -> Objects.equals(routeDestination.getApplicationGuid(), applicationGuid))
                           .noneMatch(routeDestination -> Objects.equals(routeDestination.getProtocol(),
                                                                         updatedRoute.getRequestedProtocol()));
    }

    private void validateDomainForRoute(CloudRoute route, Map<String, UUID> existingDomains) {
        String domain = route.getDomain()
                             .getName();
        if (!StringUtils.hasLength(domain) || !existingDomains.containsKey(domain)) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.getReasonPhrase(),
                                              "Domain '" + domain + "' not found for URI " + route.getUrl());
        }
    }

    // --- domain resolution (minimal inline lookups; the Domains group owns the full wire model) --

    private UUID getRequiredDomainGuid(String name) {
        UUID guid = findDomainGuidByName(name);
        if (guid == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Domain " + name + " not found.");
        }
        return guid;
    }

    private UUID findDomainGuidByName(String name) {
        List<V3Domain> domains = cc.list("/v3/domains?names=" + name + "&per_page=" + DEFAULT_PAGE_SIZE, DOMAIN_PAGE);
        if (CollectionUtils.isEmpty(domains)) {
            return null;
        }
        return UUID.fromString(domains.get(0)
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
        return cc.list("/v3/domains?names=" + names + "&per_page=" + DEFAULT_PAGE_SIZE, DOMAIN_PAGE)
                 .stream()
                 .collect(Collectors.toMap(V3Domain::name, domain -> UUID.fromString(domain.guid())));
    }

    // --- application resolution ------------------------------------------------------------------

    private UUID getRequiredApplicationGuid(String applicationName) {
        StringBuilder query = new StringBuilder("/v3/apps?per_page=" + DEFAULT_PAGE_SIZE);
        query.append("&names=")
             .append(applicationName);
        if (target != null && target.getGuid() != null) {
            query.append("&space_guids=")
                 .append(target.getGuid());
        }
        List<V3AppRef> apps = cc.list(query.toString(), new ParameterizedTypeReference<V3ListResponse<V3AppRef>>() {
        });
        if (CollectionUtils.isEmpty(apps)) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Application " + applicationName + " not found.");
        }
        return UUID.fromString(apps.get(0)
                                   .guid());
    }

    private UUID getTargetSpaceGuid() {
        return target.getGuid();
    }

    private void assertSpaceProvided(String operation) {
        if (target == null) {
            throw new IllegalArgumentException("Unable to " + operation + " without specifying organization and space to use.");
        }
    }

    /**
     * Minimal wire-model of a CF v3 application resource — only the {@code guid} needed to resolve an application by name.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record V3AppRef(@JsonProperty("guid") String guid) {
    }

}
