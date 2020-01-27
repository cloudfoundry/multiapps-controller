package com.sap.cloud.lm.sl.cf.web.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.HandlerMapping;

import com.sap.cloud.lm.sl.cf.web.message.Messages;

public final class ServletUtil {

    private static final String SINGLE_FORWARD_SLASH = "/";
    private static final String TWO_OR_MORE_FORWARD_SLASHES_REGEX = "\\/\\/+";

    private ServletUtil() {

    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> getPathVariables(ServletRequest request) {
        return (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
    }

    public static String getPathVariable(ServletRequest request, String pathVariableName) {
        Map<String, String> pathVariables = getPathVariables(request);
        return pathVariables.get(pathVariableName);
    }

    public static String decodeUri(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return decode(uri);
    }

    public static String decode(String string) {
        try {
            return URLDecoder.decode(string, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(MessageFormat.format(Messages.COULD_NOT_DECODE_STRING_0, string), e);
        }
    }

    public static String removeInvalidForwardSlashes(String uri) {
        return uri.replaceAll(TWO_OR_MORE_FORWARD_SLASHES_REGEX, SINGLE_FORWARD_SLASH);
    }

}
