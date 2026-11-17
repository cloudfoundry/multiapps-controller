package org.cloudfoundry.multiapps.controller.client.facade.util;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.common.util.MiscUtil;
import org.cloudfoundry.multiapps.controller.Constants;
import org.cloudfoundry.multiapps.controller.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.CloudException;
import org.springframework.web.util.UriUtils;

public class UriUtil {

    private UriUtil() {
        // prevents initialization
    }

    public static String encodeChars(String queryParam, List<String> charsToEncode) {
        for (String charToEncode : charsToEncode) {
            queryParam = queryParam.replaceAll(charToEncode, UriUtils.encode(charToEncode, Charset.defaultCharset()));
        }
        return queryParam;
    }

    public static URL extractBaseUrl(URL url) {
        try {
            int port = url.getPort();
            String origin = url.getProtocol() + Constants.PROTOCOL_SEPARATOR + url.getHost();

            if (port != Constants.UNDEFINED_PORT) {
                origin = origin + Constants.COLON + port;
            }

            return URI.create(origin)
                      .toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(MessageFormat.format(Messages.COULD_NOT_DETERMINE_API_ORIGIN_FROM_0, url), e);
        }
    }

    private static URL logCacheFallbackUrl(URL controllerUrl) {
        String host = controllerUrl.getHost();

        String logCacheHost = host.startsWith(Constants.API_HOST_PREFIX) ? Constants.LOG_CACHE_PREFIX + host.substring(
            Constants.API_HOST_PREFIX.length()) : Constants.LOG_CACHE_PREFIX + host;
        try {
            return URI.create(controllerUrl.getProtocol() + Constants.PROTOCOL_SEPARATOR + logCacheHost)
                      .toURL();
        } catch (MalformedURLException me) {
            throw new CloudException(MessageFormat.format(Messages.COULD_NOT_RESOLVE_LOG_CACHE_URL_FROM_0, controllerUrl), me);
        }
    }

    private static URL apiFallbackUrl(URL controllerUrl, String pathSuffix) {
        try {
            return URI.create(controllerUrl.toString() + pathSuffix)
                      .toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(
                MessageFormat.format(Messages.COULD_NOT_RESOLVE_URL_FROM_0_1, controllerUrl, pathSuffix), e);
        }
    }

    public static URL resolveV3ApiUrl(URL controllerUrl, Map<String, Object> rootDocumentLinks) {
        try {
            Map<String, Object> cloudControllerUrlV3 = MiscUtil.cast(rootDocumentLinks.get(
                Constants.CLOUD_CONTROLLER_CF_ROOT_DOCUMENT_NAME));
            return URI.create((String) cloudControllerUrlV3.get(Constants.HREF))
                      .toURL();
        } catch (Exception e) {
            return apiFallbackUrl(controllerUrl, Constants.CF_API_V3);
        }
    }

    public static URL resolveLogCacheUrl(URL controllerUrl, Map<String, Object> rootDocumentLinks) {
        try {
            Map<String, Object> logCacheUrl = MiscUtil.cast(rootDocumentLinks.get(Constants.LOG_CACHE_CF_ROOT_DOCUMENT_NAME));

            return URI.create((String) logCacheUrl.get(Constants.HREF))
                      .toURL();
        } catch (Exception e) {
            return logCacheFallbackUrl(controllerUrl);
        }
    }

}
