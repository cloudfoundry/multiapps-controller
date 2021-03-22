package org.cloudfoundry.multiapps.controller.web.configuration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.cloudfoundry.multiapps.controller.web.Constants;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;

public class CsrfAccessDeniedHandler implements AccessDeniedHandler {

    private static final String CSRF_TOKEN_REQUIRED_HEADER_VALUE = "Required";

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) {
        if (accessDeniedException instanceof InvalidCsrfTokenException || accessDeniedException instanceof MissingCsrfTokenException) {
            response.setHeader(Constants.CSRF_TOKEN, CSRF_TOKEN_REQUIRED_HEADER_VALUE);
            response.setStatus(HttpStatus.SC_FORBIDDEN);
        }
    }
}
