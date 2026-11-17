package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudRoute;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudTask;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudDomain;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudRoute;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudTask;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableRouteDestination;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationsUtilTest {

    private static final UUID APP_GUID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void testDetermineNameResolutionErrorMapsForbidden() {
        CloudOperationException original = new CloudOperationException(HttpStatus.FORBIDDEN);

        CloudOperationException result = OperationsUtil.determineNameResolutionError(original, "forbidden-message", "not-found-message");

        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
        assertTrue(result.getMessage()
                         .contains("forbidden-message"), result.getMessage());
        assertSame(original, result.getCause());
    }

    @Test
    void testDetermineNameResolutionErrorMapsNotFound() {
        CloudOperationException original = new CloudOperationException(HttpStatus.NOT_FOUND);

        CloudOperationException result = OperationsUtil.determineNameResolutionError(original, "forbidden-message", "not-found-message");

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        assertTrue(result.getMessage()
                         .contains("not-found-message"), result.getMessage());
        assertSame(original, result.getCause());
    }

    @Test
    void testDetermineNameResolutionErrorReturnsOriginalForOtherStatus() {
        CloudOperationException original = new CloudOperationException(HttpStatus.BAD_GATEWAY);

        CloudOperationException result = OperationsUtil.determineNameResolutionError(original, "forbidden-message", "not-found-message");

        assertSame(original, result);
    }

    @Test
    void testBuildCreateTaskBodyWithoutLimits() {
        CloudTask task = ImmutableCloudTask.builder()
                                           .name("my-task")
                                           .command("run.sh")
                                           .build();

        Map<String, Object> body = OperationsUtil.buildCreateTaskBody(task);

        assertEquals("run.sh", body.get("command"));
        assertEquals("my-task", body.get("name"));
        assertFalse(body.containsKey("memory_in_mb"));
        assertFalse(body.containsKey("disk_in_mb"));
    }

    @Test
    void testBuildCreateTaskBodyWithMemoryAndDiskLimits() {
        CloudTask task = ImmutableCloudTask.builder()
                                           .name("my-task")
                                           .command("run.sh")
                                           .limits(ImmutableCloudTask.ImmutableLimits.builder()
                                                                                     .memory(512)
                                                                                     .disk(1024)
                                                                                     .build())
                                           .build();

        Map<String, Object> body = OperationsUtil.buildCreateTaskBody(task);

        assertEquals(512, body.get("memory_in_mb"));
        assertEquals(1024, body.get("disk_in_mb"));
    }

    @Test
    void testBuildCreateTaskBodyWithLimitsButNullValuesOmitsThem() {
        CloudTask task = ImmutableCloudTask.builder()
                                           .name("my-task")
                                           .command("run.sh")
                                           .limits(ImmutableCloudTask.ImmutableLimits.builder()
                                                                                     .build())
                                           .build();

        Map<String, Object> body = OperationsUtil.buildCreateTaskBody(task);

        assertFalse(body.containsKey("memory_in_mb"));
        assertFalse(body.containsKey("disk_in_mb"));
    }

    @Test
    void testBuildCreateTaskBodyWithOnlyMemoryLimit() {
        CloudTask task = ImmutableCloudTask.builder()
                                           .name("my-task")
                                           .command("run.sh")
                                           .limits(ImmutableCloudTask.ImmutableLimits.builder()
                                                                                     .memory(256)
                                                                                     .build())
                                           .build();

        Map<String, Object> body = OperationsUtil.buildCreateTaskBody(task);

        assertEquals(256, body.get("memory_in_mb"));
        assertFalse(body.containsKey("disk_in_mb"));
    }

    @Test
    void testThrowOnErrorsDoesNothingForEmptyList() {
        OperationsUtil.throwOnErrors(List.of());
    }

    @Test
    void testThrowOnErrorsThrowsFirstAndSuppressesRest() {
        CloudOperationException first = new CloudOperationException(HttpStatus.BAD_GATEWAY);
        CloudOperationException second = new CloudOperationException(HttpStatus.NOT_FOUND);
        CloudOperationException third = new CloudOperationException(HttpStatus.INTERNAL_SERVER_ERROR);

        CloudOperationException thrown = assertThrows(CloudOperationException.class,
                                                      () -> OperationsUtil.throwOnErrors(List.of(first, second, third)));

        assertSame(first, thrown);
        assertEquals(2, thrown.getSuppressed().length);
        assertSame(second, thrown.getSuppressed()[0]);
        assertSame(third, thrown.getSuppressed()[1]);
    }

    @Test
    void testThrowOnErrorsWithSingleErrorThrowsItWithoutSuppressed() {
        CloudOperationException only = new CloudOperationException(HttpStatus.BAD_REQUEST);

        CloudOperationException thrown = assertThrows(CloudOperationException.class,
                                                      () -> OperationsUtil.throwOnErrors(List.of(only)));

        assertSame(only, thrown);
        assertEquals(0, thrown.getSuppressed().length);
    }

    @Test
    void testGetNewRoutesIncludesRouteWhenRequestedProtocolDiffersFromDestination() {
        CloudRoute current = routeWithDestination("app.example.com", "http1");
        CloudRoute updated = routeWithRequestedProtocol("app.example.com", "http2");

        Set<CloudRoute> newRoutes = OperationsUtil.getNewRoutes(APP_GUID, List.of(current), Set.of(updated));

        assertEquals(1, newRoutes.size());
    }

    @Test
    void testGetNewRoutesExcludesRouteWhenRequestedProtocolMatchesDestination() {
        CloudRoute current = routeWithDestination("app.example.com", "http2");
        CloudRoute updated = routeWithRequestedProtocol("app.example.com", "http2");

        Set<CloudRoute> newRoutes = OperationsUtil.getNewRoutes(APP_GUID, List.of(current), Set.of(updated));

        assertTrue(newRoutes.isEmpty());
    }

    @Test
    void testGetNewRoutesExcludesRouteWhenRequestedProtocolIsNull() {
        CloudRoute current = routeWithDestination("app.example.com", "http1");
        CloudRoute updated = routeWithRequestedProtocol("app.example.com", null);

        Set<CloudRoute> newRoutes = OperationsUtil.getNewRoutes(APP_GUID, List.of(current), Set.of(updated));

        assertTrue(newRoutes.isEmpty());
    }

    @Test
    void testGetNewRoutesIncludesRouteWithNoMatchingCurrentRoute() {
        CloudRoute updated = routeWithRequestedProtocol("brand-new.example.com", "http1");

        Set<CloudRoute> newRoutes = OperationsUtil.getNewRoutes(APP_GUID, List.of(), Set.of(updated));

        assertEquals(1, newRoutes.size());
    }

    @Test
    void testValidateDomainForRoutePassesWhenDomainExists() {
        CloudRoute route = routeOnDomain("example.com");

        OperationsUtil.validateDomainForRoute(route, Map.of("example.com", UUID.randomUUID()));
    }

    @Test
    void testValidateDomainForRouteThrowsWhenDomainNotInExistingDomains() {
        CloudRoute route = routeOnDomain("missing.example.com");

        CloudOperationException thrown = assertThrows(CloudOperationException.class,
                                                      () -> OperationsUtil.validateDomainForRoute(route, Map.of("example.com",
                                                                                                                UUID.randomUUID())));

        assertEquals(HttpStatus.NOT_FOUND, thrown.getStatusCode());
    }

    @Test
    void testValidateDomainForRouteThrowsWhenDomainNameIsEmpty() {
        CloudRoute route = routeOnDomain("");

        assertThrows(CloudOperationException.class,
                     () -> OperationsUtil.validateDomainForRoute(route, Map.of("example.com", UUID.randomUUID())));
    }

    private static CloudRoute routeWithDestination(String url, String destinationProtocol) {
        return ImmutableCloudRoute.builder()
                                  .url(url)
                                  .host("app")
                                  .domain(ImmutableCloudDomain.builder()
                                                              .name("example.com")
                                                              .build())
                                  .addDestinations(ImmutableRouteDestination.builder()
                                                                            .applicationGuid(APP_GUID)
                                                                            .protocol(destinationProtocol)
                                                                            .build())
                                  .build();
    }

    private static CloudRoute routeWithRequestedProtocol(String url, String requestedProtocol) {
        return ImmutableCloudRoute.builder()
                                  .url(url)
                                  .host("app")
                                  .domain(ImmutableCloudDomain.builder()
                                                              .name("example.com")
                                                              .build())
                                  .requestedProtocol(requestedProtocol)
                                  .build();
    }

    private static CloudRoute routeOnDomain(String domainName) {
        return ImmutableCloudRoute.builder()
                                  .url(domainName.isEmpty() ? "app" : "app." + domainName)
                                  .host("app")
                                  .domain(ImmutableCloudDomain.builder()
                                                              .name(domainName)
                                                              .build())
                                  .build();
    }
}
