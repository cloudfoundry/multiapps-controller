package org.cloudfoundry.multiapps.controller.core.util;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.common.NotFoundException;
import org.cloudfoundry.multiapps.controller.core.Messages;

import com.sap.cloudfoundry.client.facade.domain.CloudRoute;

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

    public static String prettyPrintRoutes(Collection<CloudRoute> routes) {
        return routes.stream()
                     .map(CloudRoute::getUrl)
                     .collect(Collectors.joining(", "));
    }

    public static void validateUrl(String url) {
        try {
            new URL(url).toURI();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new IllegalArgumentException(MessageFormat.format(Messages.INVALID_URL, url), e);
        }
    }

    public static boolean isUrlSecure(String url) {
        return url.startsWith(HTTPS_PROTOCOL);
    }
}
