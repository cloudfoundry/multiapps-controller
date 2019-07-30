package com.sap.cloud.lm.sl.cf.core.util;

import java.util.List;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.common.NotFoundException;
import org.cloudfoundry.client.lib.domain.CloudRoute;

public class UriUtil {

    public static final String DEFAULT_SCHEME_SEPARATOR = "://";
    public static final char DEFAULT_PATH_SEPARATOR = '/';
    public static final char DEFAULT_HOST_DOMAIN_SEPARATOR = '.';

    public static final int STANDARD_HTTP_PORT = 80;
    public static final int STANDARD_HTTPS_PORT = 443;

    public static final String HTTP_PROTOCOL = "http";
    public static final String HTTPS_PROTOCOL = "https";

    private UriUtil() {
    }

    public static String stripScheme(String uri) {
        int protocolIndex = uri.indexOf(DEFAULT_SCHEME_SEPARATOR);
        if (protocolIndex == -1) {
            return uri;
        }
        return uri.substring(protocolIndex + DEFAULT_SCHEME_SEPARATOR.length());
    }

    public static CloudRoute findRoute(List<CloudRoute> routes, String uri) {
        return routes.stream()
            .filter(route -> routeMatchesUri(route, uri))
            .findAny()
            .orElseThrow(() -> new NotFoundException(Messages.ROUTE_NOT_FOUND, uri));
    }

    public static boolean routeMatchesUri(CloudRoute route, String uri) {
        ApplicationURI appUri = new ApplicationURI(uri);
        return route.getHost()
            .equals(appUri.getHost())
            && route.getDomain()
                .getName()
                .equals(appUri.getDomain());
    }
}
