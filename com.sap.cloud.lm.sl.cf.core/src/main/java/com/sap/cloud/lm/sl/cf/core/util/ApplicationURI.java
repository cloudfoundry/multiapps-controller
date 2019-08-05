package com.sap.cloud.lm.sl.cf.core.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;

public class ApplicationURI {

    private String uri;
    private String host;
    private String domain;
    private String path;

    public static String getDomainFromURI(String uri) {
        ApplicationURI parsedURI = new ApplicationURI(uri);
        return parsedURI.getDomain();
    }

    public ApplicationURI(String initial) {
        uri = UriUtil.stripScheme(initial);

        int domainIndex = uri.indexOf(UriUtil.DEFAULT_HOST_DOMAIN_SEPARATOR);
        int pathIndex = getPathIndexAfter(domainIndex);

        if (domainIndex > 0) {
            setHost(uri.substring(0, domainIndex));
            setDomain(uri.substring(domainIndex + 1, pathIndex));
        } else {
            setHost("");
            setDomain(uri.substring(0, pathIndex));
        }

        if (pathIndex < uri.length()) {
            setPath(uri.substring(pathIndex));
        }
    }

    private int getPathIndexAfter(int pos) {
        int pathIndex = uri.indexOf(UriUtil.DEFAULT_PATH_SEPARATOR, pos);
        if (pathIndex < 0) {
            pathIndex = uri.length();
        }
        return pathIndex;
    }

    public Map<String, Object> getURIParts() {
        Map<String, Object> uriParts = new HashMap<>();

        uriParts.put(SupportedParameters.HOST, getHost());
        uriParts.put(SupportedParameters.DOMAIN, getDomain());
        uriParts.put(SupportedParameters.ROUTE_PATH, getPath());

        return Collections.unmodifiableMap(uriParts);
    }

    public Object getURIPart(String partName) {
        switch (partName) {
            case SupportedParameters.HOST:
                return getHost();
            case SupportedParameters.DOMAIN:
                return getDomain();
            case SupportedParameters.ROUTE_PATH:
                return getPath();
            default:
                return null;
        }
    }

    public void setURIPart(String partName, String part) {
        switch (partName) {
            case SupportedParameters.HOST:
                setHost(part);
                break;
            case SupportedParameters.DOMAIN:
                setDomain(part);
                break;
            case SupportedParameters.ROUTE_PATH:
                setPath(part);
                break;
            default:
                // In default case just exit the method without setting anything
        }
    }

    @Override
    public String toString() {
        StringBuilder url = new StringBuilder();

        if (StringUtils.isNotEmpty(getHost())) {
            url.append(getHost())
               .append(UriUtil.DEFAULT_HOST_DOMAIN_SEPARATOR);
        }

        url.append(getDomain());

        if (StringUtils.isNotEmpty(getPath())) {
            url.append(getPath());
        }

        return url.toString();
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

}
