package com.sap.cloud.lm.sl.cf.web.security;

import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;

@Named
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    protected static final String CSRF_TOKEN_REQUIRED_HEADER_VALUE = "Required";

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) {
        if (accessDeniedException instanceof InvalidCsrfTokenException || accessDeniedException instanceof MissingCsrfTokenException) {
            response.setHeader(CsrfHeaders.CSRF_TOKEN_HEADER, CSRF_TOKEN_REQUIRED_HEADER_VALUE);
        }
        response.setStatus(HttpStatus.SC_FORBIDDEN);
    }

}
