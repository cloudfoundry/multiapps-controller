package com.sap.cloud.lm.sl.cf.core.util;

import java.util.Collections;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.cloudfoundry.client.lib.domain.ImmutableCloudDomain;
import org.cloudfoundry.client.lib.domain.ImmutableCloudRoute;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sap.cloud.lm.sl.common.NotFoundException;

public class UriUtilTest {

    private static final String HOST_BASED_URI_WITH_PORT = "https://valid-host.valid-domain:4000";
    private static final String HOST_BASED_URI_WITHOUT_PORT = "https://valid-host.valid-domain";
    private static final String PORT_BASED_URI = "https://valid-domain:4000";
    private static final String PORT_BASED_URI_WITHOUT_SCHEME = "valid-domain:4000";

    private final CloudRoute route = ImmutableCloudRoute.builder()
                                                  .host("valid-host")
                                                  .domain(ImmutableCloudDomain.builder()
                                                                              .name("valid-domain")
                                                                              .build())
                                                  .build();

    @Test
    public void testFindRouteWithHostBasedUriWithPort() {
        List<CloudRoute> routes = Collections.singletonList(route);
        Assertions.assertThrows(NotFoundException.class, () -> UriUtil.findRoute(routes, HOST_BASED_URI_WITH_PORT));
    }

    @Test
    public void testFindRouteWithHostBasedUriWithoutPort() {
        List<CloudRoute> routes = Collections.singletonList(route);
        CloudRoute actualResult = UriUtil.findRoute(routes, HOST_BASED_URI_WITHOUT_PORT);
        Assertions.assertEquals(route, actualResult);
    }

    @Test
    public void testFindRouteWithPortBasedUri() {
        List<CloudRoute> routes = Collections.singletonList(route);
        Assertions.assertThrows(NotFoundException.class, () -> UriUtil.findRoute(routes, PORT_BASED_URI));
    }

    @Test
    public void testRouteMatchesWithHostBasedUriWithPort() {
        boolean actualResult = UriUtil.routeMatchesUri(route, HOST_BASED_URI_WITH_PORT);
        Assertions.assertFalse(actualResult);
    }

    @Test
    public void testRouteMatchesWithHostBasedUriWithoutPort() {
        boolean actualResult = UriUtil.routeMatchesUri(route, HOST_BASED_URI_WITHOUT_PORT);
        Assertions.assertTrue(actualResult);
    }

    @Test
    public void testRouteMatchesWithPortBasedUri() {
        boolean actualResult = UriUtil.routeMatchesUri(route, PORT_BASED_URI);
        Assertions.assertFalse(actualResult);
    }

    @Test
    public void testStripSchemeWithScheme() {
        String actual = UriUtil.stripScheme(PORT_BASED_URI);
        Assertions.assertEquals(PORT_BASED_URI_WITHOUT_SCHEME, actual);
    }

    @Test
    public void testStripSchemeWithoutScheme() {
        String actual = UriUtil.stripScheme(PORT_BASED_URI_WITHOUT_SCHEME);
        Assertions.assertEquals(PORT_BASED_URI_WITHOUT_SCHEME, actual);
    }
}
