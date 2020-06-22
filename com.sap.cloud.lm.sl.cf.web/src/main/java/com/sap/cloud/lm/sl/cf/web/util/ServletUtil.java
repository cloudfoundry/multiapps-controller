package com.sap.cloud.lm.sl.cf.web.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UriUtils;

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
        return UriUtils.decode(string, StandardCharsets.UTF_8.name());
    }

    public static String removeInvalidForwardSlashes(String uri) {
        return uri.replaceAll(TWO_OR_MORE_FORWARD_SLASHES_REGEX, SINGLE_FORWARD_SLASH);
    }

    public static void send(HttpServletResponse response, int statusCode, String body) throws IOException {
        response.setStatus(statusCode);
        try (PrintWriter writer = response.getWriter()) {
            writer.print(body);
            writer.flush();
        }
    }

}
