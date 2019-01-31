package com.sap.cloud.lm.sl.cf.core.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.PortValidator;

public class ApplicationURI {

    private static final PortValidator PORT_VALIDATOR = new PortValidator();

    public static final String DEFAULT_SCHEME_SEPARATOR = "://";
    public static final char DEFAULT_PATH_SEPARATOR = '/';
    public static final char DEFAULT_HOST_DOMAIN_SEPARATOR = '.';
    public static final char DEFAULT_PORT_SEPARATOR = ':';

    public static final String UNPARSABLE_PORT_PARAMETER = "unparsable-port";
    public static final String SCHEME_PARAMETER = "scheme";

    public static final int STANDARD_HTTP_PORT = 80;
    public static final int STANDARD_HTTPS_PORT = 443;

    public static final String TCP_PROTOCOL = "tcp";
    public static final String TCPS_PROTOCOL = "tcps";
    public static final String HTTP_PROTOCOL = "http";
    public static final String HTTPS_PROTOCOL = "https";

    private String uri;
    private boolean isHostBased;

    private String scheme;
    private String host;
    private String domain;
    private String unparsablePort;
    private Integer port;
    private String path;

    public static Integer getPortFromURI(String uri) {
        ApplicationURI parsedURI = new ApplicationURI(uri);

        return parsedURI.getParsedPort();
    }

    public static String getDomainFromURI(String uri) {
        ApplicationURI parsedURI = new ApplicationURI(uri);

        return parsedURI.getDomain();
    }

    public ApplicationURI(String initial) {
        // uriParts = new HashMap<>();
        uri = stripScheme(initial);

        int portIndex = uri.lastIndexOf(DEFAULT_PORT_SEPARATOR);
        isHostBased = portIndex == -1;

        if (isHostBased) {
            initHostBased();
        } else {
            initPortBased(portIndex);
        }
    }

    private void initHostBased() {
        isHostBased = true;

        int domainIndex = uri.indexOf(DEFAULT_HOST_DOMAIN_SEPARATOR);
        int pathIndex = UriUtil.getPathIndexAfter(uri, domainIndex);

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

    private void initPortBased(int portIndex) {
        isHostBased = false;

        int pathIndex = UriUtil.getPathIndexAfter(uri, portIndex);

        setDomain(uri.substring(0, portIndex));
        String portString = uri.substring(portIndex + 1, pathIndex);
        try {
            setPort(Integer.parseInt(portString));
        } catch (NumberFormatException e) {
            setUnparsablePort(portString);
        }

        if (pathIndex < uri.length()) {
            setPath(uri.substring(pathIndex));
        }
    }

    private String stripScheme(String uri) {
        int protocolIndex = uri.indexOf(DEFAULT_SCHEME_SEPARATOR);
        if (protocolIndex == -1) {
            return uri;
        }

        setScheme(uri.substring(0, protocolIndex));
        return uri.substring(protocolIndex + DEFAULT_SCHEME_SEPARATOR.length());
    }

    public Map<String, Object> getURIParts() {
        Map<String, Object> uriParts = new HashMap<>();

        uriParts.put(SCHEME_PARAMETER, getScheme());
        uriParts.put(SupportedParameters.HOST, getHost());
        uriParts.put(SupportedParameters.PORT, getPort());
        uriParts.put(SupportedParameters.DOMAIN, getDomain());
        uriParts.put(SupportedParameters.ROUTE_PATH, getPath());

        return Collections.unmodifiableMap(uriParts);
    }

    public Object getURIPart(String partName) {

        switch (partName) {
            case SCHEME_PARAMETER:
                return getScheme();
            case SupportedParameters.HOST:
                return getHost();
            case SupportedParameters.PORT:
                return getPort();
            case SupportedParameters.DOMAIN:
                return getDomain();
            case SupportedParameters.ROUTE_PATH:
                return getPath();
        }

        return null;
    }

    public void setURIPart(String partName, String part) {
        switch (partName) {
            case SCHEME_PARAMETER:
                setScheme(part);
                break;
            case SupportedParameters.HOST:
                setHost(part);
                break;
            case SupportedParameters.PORT:
                setPort((Object) part);
                break;
            case SupportedParameters.DOMAIN:
                setDomain(part);
                break;
            case SupportedParameters.ROUTE_PATH:
                setPath(part);
                break;
        }

    }

    public String toString() {
        return toString(false);
    }

    public String toString(boolean hideValidPort) {
        StringBuffer url = new StringBuffer();

        if (StringUtils.isNotEmpty(getScheme())) {
            url.append(getScheme())
                .append(ApplicationURI.DEFAULT_SCHEME_SEPARATOR);
        }

        if (isHostBased && StringUtils.isNotEmpty(getHost())) {
            url.append(getHost())
                .append(ApplicationURI.DEFAULT_HOST_DOMAIN_SEPARATOR);
        }

        url.append(getDomain());

        if (!isHostBased) {
            boolean skipPort = hideValidPort && getParsedPort() != null;
            if (!skipPort && getPort() != null) {
                url.append(ApplicationURI.DEFAULT_PORT_SEPARATOR)
                    .append(getPort());
            }
        }

        if (StringUtils.isNotEmpty(getPath())) {
            url.append(getPath());
        }

        return url.toString();
    }

    public boolean isTcpOrTcps() {
        return getScheme() != null && (getScheme().equals(TCP_PROTOCOL) || getScheme().equals(TCPS_PROTOCOL));
    }

    public boolean hasValidPort() {
        return PORT_VALIDATOR.isValid(getParsedPort()) && StringUtils.isEmpty(getUnparsablePort());
    }

    public boolean hasStandardPort(String protocol) {
        if (getPort() == null) {
            return false;
        }

        return protocol.equals(HTTP_PROTOCOL) && getParsedPort() == STANDARD_HTTP_PORT
            || protocol.equals(HTTPS_PROTOCOL) && getParsedPort() == STANDARD_HTTPS_PORT;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        if (StringUtils.isNotEmpty(getUnparsablePort())) {
            return getUnparsablePort();
        }

        if (getParsedPort() != null) {
            return getParsedPort().toString();
        }

        return null;
    }

    public Integer getParsedPort() {
        return port;
    }

    public String getUnparsablePort() {
        return unparsablePort;
    }

    public void setPort(Object port) {
        if (port instanceof Integer) {
            setPort((Integer) port);
        } else if (port instanceof String) {
            setUnparsablePort((String) port);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public void setPort(Integer port) {
        this.port = port;
        this.unparsablePort = null;
    }

    public void setUnparsablePort(String unparsablePort) {
        this.unparsablePort = unparsablePort;
        this.port = null;
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

    public boolean isHostBased() {
        return isHostBased;
    }

}
