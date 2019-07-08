package com.sap.cloud.lm.sl.cf.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.sap.cloud.lm.sl.common.NotFoundException;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.cloudfoundry.client.lib.domain.ImmutableCloudDomain;
import org.cloudfoundry.client.lib.domain.ImmutableCloudRoute;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class UriUtilTest {

    private static final String HOST_BASED_URI_WITH_PORT = "https://valid-host.valid-domain:4000";
    private static final String HOST_BASED_URI_WITHOUT_PORT = "https://valid-host.valid-domain";
    private static final String PORT_BASED_URI = "https://valid-domain:4000";
    private static final String PORT_BASED_URI_WITHOUT_SCHEME = "valid-domain:4000";

    private CloudRoute route = ImmutableCloudRoute.builder()
        .host("valid-host")
        .domain(ImmutableCloudDomain.builder()
            .name("valid-domain")
            .build())
        .build();

    @Test(expected = NotFoundException.class)
    public void testFindRouteWithHostBasedUriWithPort() {
        List<CloudRoute> routes = Arrays.asList(route);
        UriUtil.findRoute(routes, HOST_BASED_URI_WITH_PORT);
    }

    @Test
    public void testFindRouteWithHostBasedUriWithoutPort() {
        List<CloudRoute> routes = Arrays.asList(route);
        CloudRoute actualResult = UriUtil.findRoute(routes, HOST_BASED_URI_WITHOUT_PORT);
        assertEquals(route, actualResult);
    }

    @Test(expected = NotFoundException.class)
    public void testFindRouteWithPortBasedUri() {
        List<CloudRoute> routes = Arrays.asList(route);
        UriUtil.findRoute(routes, PORT_BASED_URI);
    }

    @Test
    public void testRouteMatchesWithHostBasedUriWithPort() {
        boolean actualResult = UriUtil.routeMatchesUri(route, HOST_BASED_URI_WITH_PORT);
        assertFalse(actualResult);
    }

    @Test
    public void testRouteMatchesWithHostBasedUriWithoutPort() {
        boolean actualResult = UriUtil.routeMatchesUri(route, HOST_BASED_URI_WITHOUT_PORT);
        assertTrue(actualResult);
    }

    @Test
    public void testRouteMatchesWithPortBasedUri() {
        boolean actualResult = UriUtil.routeMatchesUri(route, PORT_BASED_URI);
        assertFalse(actualResult);
    }

    @Test
    public void testStripSchemeWithScheme() {
        String actual = UriUtil.stripScheme(PORT_BASED_URI);
        assertEquals(PORT_BASED_URI_WITHOUT_SCHEME, actual);
    }

    @Test
    public void testStripSchemeWithoutScheme() {
        String actual = UriUtil.stripScheme(PORT_BASED_URI_WITHOUT_SCHEME);
        assertEquals(PORT_BASED_URI_WITHOUT_SCHEME, actual);
    }
}
