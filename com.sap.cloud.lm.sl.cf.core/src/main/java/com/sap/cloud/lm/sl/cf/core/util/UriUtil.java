package com.sap.cloud.lm.sl.cf.core.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudRoute;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.util.CommonUtil;
import com.sap.cloud.lm.sl.common.util.Pair;

public class UriUtil {

    public static final String DEFAULT_SCHEME_SEPARATOR = "://";
    public static final char DEFAULT_PATH_SEPARATOR = '/';
    public static final char DEFAULT_HOST_DOMAIN_SEPARATOR = '.';

    public static final String HTTP_PROTOCOL = "http";
    public static final String HTTPS_PROTOCOL = "https";

    public static Map<String, Object> splitUri(String uri) {
        Map<String, Object> splitUri = new HashMap<>();

        uri = getUriWithoutScheme(uri);
        int pathIndex = uri.length();

        int domainIndex = uri.indexOf(DEFAULT_HOST_DOMAIN_SEPARATOR);
        pathIndex = getPathIndexAfter(uri, domainIndex);
        if (domainIndex > 0) {
            splitUri.put(SupportedParameters.HOST, uri.substring(0, domainIndex));
            splitUri.put(SupportedParameters.DOMAIN, uri.substring(domainIndex + 1, pathIndex));
        } else {
            splitUri.put(SupportedParameters.HOST, "");
            splitUri.put(SupportedParameters.DOMAIN, uri.substring(0, pathIndex));
        }
        return splitUri;
    }

    public static Map<String, Object> splitUriWithPath(String uri) {
        Map<String, Object> splitUri = splitUri(uri);

        String domain = (String) splitUri.get(SupportedParameters.HOST);
        int pathIndex = domain.indexOf(DEFAULT_PATH_SEPARATOR);
        if (pathIndex > 0) {
            splitUri.put(SupportedParameters.HOST, domain.substring(0, pathIndex));
            splitUri.put(SupportedParameters.ROUTE_PATH, domain.substring(pathIndex));
        }

        return splitUri;
    }

    // TODO: move this in a new utility class, also see what's up with the scheme really
    public static String buildUri(String scheme, Map<String, Object> uriParts) {
        return buildUri(scheme, (String) uriParts.get(SupportedParameters.HOST), (String) uriParts.get(SupportedParameters.DOMAIN), (String) uriParts.get(SupportedParameters.ROUTE_PATH));
    }

    public static String buildUri(String scheme, String host, String domain, String path) {
        StringBuilder uri = new StringBuilder();
        if (!CommonUtil.isNullOrEmpty(scheme)) {
            uri.append(scheme)
                .append(UriUtil.DEFAULT_SCHEME_SEPARATOR);
        }
        if (!CommonUtil.isNullOrEmpty(host)) {
            uri.append(host)
                .append(UriUtil.DEFAULT_HOST_DOMAIN_SEPARATOR);
        }

        uri.append(domain);

        if (!CommonUtil.isNullOrEmpty(path)) {
            uri.append(path);
        }

        return uri.toString();
    }

    public static Pair<String, String> getHostAndDomain(String uri) {
        uri = getUriWithoutScheme(uri);

        int domainIndex = uri.indexOf(DEFAULT_HOST_DOMAIN_SEPARATOR);
        int pathIndex = getPathIndexAfter(uri, domainIndex);
        if (domainIndex > 0) {
            return new Pair<>(uri.substring(0, domainIndex), uri.substring(domainIndex + 1, pathIndex));
        }
        return new Pair<>("", uri.substring(0, pathIndex));
    }

    public static String getDomain(String uri) {
        try {
            String domain = (String) splitUri(uri).get(SupportedParameters.DOMAIN);
            if (!CommonUtil.isNullOrEmpty(domain)) {
                return domain;
            }
            return null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected static int getPathIndexAfter(String uri, int pos) {
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

    public static String getScheme(String uri) {
        String scheme = "";
        int protocolIndex = uri.indexOf(DEFAULT_SCHEME_SEPARATOR);
        if (protocolIndex > 0)
            scheme = uri.substring(0, protocolIndex);
        return scheme;
    }

    public static CloudRoute findRoute(List<CloudRoute> routes, String uri) {
        return routes.stream()
            .filter(route -> routeMatchesUri(route, uri))
            .findAny()
            .orElseThrow(() -> new NotFoundException(Messages.ROUTE_NOT_FOUND, uri));
    }

    public static boolean routeMatchesUri(CloudRoute route, String uri) {
        Pair<String, String> hostAndDomain = UriUtil.getHostAndDomain(uri);
        String host = hostAndDomain._1;
        String domain = hostAndDomain._2;
        return route.getHost()
            .equals(host)
            && route.getDomain()
                .getName()
                .equals(domain);
    }

}
