package org.cloudfoundry.multiapps.controller.web.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface UriAuthorizationFilter {

    String getUriRegex();

    /**
     * @return Whether or not the request should be forwarded to the rest of the filter chain and eventually to the appropriate handler.
     */
    boolean ensureUserIsAuthorized(HttpServletRequest request, HttpServletResponse response) throws IOException;
}
