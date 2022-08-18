package org.cloudfoundry.multiapps.controller.core.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;

import com.sap.cloudfoundry.client.facade.domain.CloudRoute;

public class TestData {

    public static Map<String, Object> routeParameterWithAdditionalValues(String route, boolean noHostname,
                                                                         Map<String, Object> additionalParameters) {
        Map<String, Object> resultParameter = constructRouteParameter(route, false, noHostname);

        if (additionalParameters != null) {
            resultParameter.putAll(additionalParameters);
        }

        return resultParameter;
    }

    public static Map<String, Object> routeParameter(String route) {
        return constructRouteParameter(route, false, null);
    }

    public static Map<String, Object> idleRouteParameter(String route) {
        return constructRouteParameter(route, true, null);
    }

    public static Map<String, Object> routeParameter(String route, Boolean noHostname) {
        return constructRouteParameter(route, false, noHostname);
    }

    public static Map<String, Object> idleRouteParameter(String route, Boolean noHostname) {
        return constructRouteParameter(route, true, noHostname);
    }

    public static Map<String, Object> constructRouteParameter(String route, boolean isIdle, Boolean noHostname) {
        Map<String, Object> resultMap = new HashMap<>();

        resultMap.put(isIdle ? SupportedParameters.IDLE_ROUTE : SupportedParameters.ROUTE, route);
        if (noHostname != null) {
            resultMap.put(SupportedParameters.NO_HOSTNAME, noHostname);
        }

        return resultMap;
    }

    // prefix any uri String with NOHOSTNAME_TEST_PREFIX to parse as hostless CloudRoute; makes test input more readable
    public static final String NOHOSTNAME_URI_FLAG = "NOHOSTNAME-";

    public static CloudRoute route(String uri) {
        return route(removePrefix(uri), uriIsHostless(uri));
    }

    public static CloudRoute route(String uri, boolean noHostname) {
        return new ApplicationURI(uri, noHostname).toCloudRoute();
    }

    public static CloudRoute route(String host, String domain, String path) {
        return new ApplicationURI(host, domain, path).toCloudRoute();
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
