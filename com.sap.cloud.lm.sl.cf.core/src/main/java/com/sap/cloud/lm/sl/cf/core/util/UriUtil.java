package com.sap.cloud.lm.sl.cf.core.util;

import java.util.List;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.domain.CloudRoute;

import com.sap.cloud.lm.sl.cf.core.validators.parameters.PortValidator;
import com.sap.cloud.lm.sl.common.util.Pair;

public class UriUtil {

    private static final String DEFAULT_SCHEME_SEPARATOR = "://";
    private static final char DEFAULT_PATH_SEPARATOR = '/';
    private static final char DEFAULT_HOST_DOMAIN_SEPARATOR = '.';
    private static final char DEFAULT_PORT_SEPARATOR = ':';
    private static final PortValidator PORT_VALIDATOR = new PortValidator();

    public static final int STANDARD_HTTP_PORT = 80;
    public static final int STANDARD_HTTPS_PORT = 443;

    public static Pair<String, String> getHostAndDomain(String uri) {
        uri = getUriWithoutScheme(uri);

        int portIndex = uri.lastIndexOf(DEFAULT_PORT_SEPARATOR);
        if (portIndex > 0) {
            int pathIndex = getPathIndexAfter(uri, portIndex);
            // Port-based route, return (port, domain):
            return new Pair<>(uri.substring(portIndex + 1, pathIndex), uri.substring(0, portIndex));
        } else {
            // Host-based route, return (host, domain):
            int domainIndex = uri.indexOf(DEFAULT_HOST_DOMAIN_SEPARATOR);
            int pathIndex = getPathIndexAfter(uri, domainIndex);
            if (domainIndex > 0) {
                return new Pair<>(uri.substring(0, domainIndex), uri.substring(domainIndex + 1, pathIndex));
            }
            return new Pair<>("", uri.substring(0, pathIndex));
        }
    }

    public static Integer getPort(String uri) {
        try {
            int port = Integer.parseInt(getHostAndDomain(uri)._1);
            if (isValidPort(port)) {
                return port;
            }
            return null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int getPathIndexAfter(String uri, int pos) {
        int pathIndex = uri.indexOf(DEFAULT_PATH_SEPARATOR, pos);
        if (pathIndex < 0) {
            pathIndex = uri.length();
        }
        return pathIndex;
    }

    public static String getPath(String uri) {
        uri = getUriWithoutScheme(uri);
        int pathIndex = uri.indexOf(DEFAULT_PATH_SEPARATOR);
        if (pathIndex < 0) {
            return null;
        }
        return uri.substring(pathIndex);
    }

    public static String getUriWithoutScheme(String uri) {
        int protocolIndex = uri.indexOf(DEFAULT_SCHEME_SEPARATOR);
        if (protocolIndex > 0)
            uri = uri.substring(protocolIndex + DEFAULT_SCHEME_SEPARATOR.length());
        return uri;
    }

    public static List<String> getUrisWithoutScheme(List<String> uris) {
        return uris.stream().map(uri -> getUriWithoutScheme(uri)).collect(Collectors.toList());
    }

    public static boolean isValidPort(int port) {
        return PORT_VALIDATOR.isValid(port);
    }

    public static String removePort(String uri) {
        int portIndex = uri.lastIndexOf(DEFAULT_PORT_SEPARATOR);
        if (portIndex < 0) {
            return uri;
        }
        int pathIndex = getPathIndexAfter(uri, portIndex);
        String port = uri.substring(portIndex + 1, pathIndex);
        try {
            Integer.parseInt(port);
        } catch (NumberFormatException e) {
            int schemaIndex = uri.indexOf(DEFAULT_PORT_SEPARATOR);
            if (port.equals("") && portIndex != schemaIndex) {
                return uri.substring(0, portIndex) + uri.substring(portIndex + 1, uri.length());
            }
            return uri;
        }

        return uri.replaceFirst(DEFAULT_PORT_SEPARATOR + port, "");
    }

    public static boolean isStandardPort(int port, String protocol) {
        return protocol.equals("http") && port == STANDARD_HTTP_PORT || protocol.equals("https") && port == STANDARD_HTTPS_PORT;
    }

    public static CloudRoute findRoute(List<CloudRoute> routes, String uri, boolean isPortBasedRouting) {
        return routes.stream().filter(route -> routeMatchesUri(route, uri, isPortBasedRouting)).findAny().orElseThrow(
            () -> new IllegalStateException("No matching route found for URI: " + uri));
    }

    public static boolean routeMatchesUri(CloudRoute route, String uri, boolean isPortBasedRouting) {
        Pair<String, String> hostAndDomain;
        if (isPortBasedRouting) {
            hostAndDomain = UriUtil.getHostAndDomain(uri);
        } else {
            hostAndDomain = UriUtil.getHostAndDomain(UriUtil.removePort(uri));
        }
        String host = hostAndDomain._1;
        String domain = hostAndDomain._2;
        return route.getHost().equals(host) && route.getDomain().getName().equals(domain);
    }

}
