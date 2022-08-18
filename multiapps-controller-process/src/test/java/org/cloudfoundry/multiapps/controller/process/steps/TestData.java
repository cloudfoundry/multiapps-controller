package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationURI;

import com.sap.cloudfoundry.client.facade.domain.CloudRoute;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudDomain;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudRoute;

// TODO: this class repeats code from test class org.cloudfoundry.multiapps.controller.core.util.TestData; remove if we refactor core module and stplit ApplicationUri out of it
public class TestData {

    // prefix any uri String with NOHOSTNAME_TEST_PREFIX to parse as hostless CloudRoute; makes test input more readable
    public static final String NOHOSTNAME_URI_FLAG = "NOHOSTNAME-";

    public static CloudRoute route(String uri) {
        return route(removePrefix(uri), uriIsHostless(uri));
    }

    public static CloudRoute route(String uri, boolean noHostname) {
        return new ApplicationURI(uri, noHostname).toCloudRoute();
    }

    public static CloudRoute route(String host, String domain, String path) {
        return ImmutableCloudRoute.builder()
                                  .host(host)
                                  .domain(ImmutableCloudDomain.builder()
                                                              .name(domain)
                                                              .build())
                                  .path(path)
                                  .build();
    }

    public static Set<CloudRoute> routeSet(List<String> uriStrings) {
        return routeSet((String[]) uriStrings.toArray());
    }

    public static Set<CloudRoute> routeSet(String... uriStrings) {
        return Stream.of(uriStrings)
                     .map(TestData::route)
                     .collect(Collectors.toSet());
    }

    private static String removePrefix(String uri) {
        if (!uriIsHostless(uri)) {
            return uri;
        }

        return uri.substring(NOHOSTNAME_URI_FLAG.length());
    }

    private static boolean uriIsHostless(String uri) {
        return uri.startsWith(NOHOSTNAME_URI_FLAG);
    }
}
