package com.sap.cloud.lm.sl.cf.web.security;

import java.io.IOException;

import javax.inject.Named;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sap.cloud.lm.sl.cf.web.Constants;

/**
 * A filter that is added in the Spring filter chain after the CSRF_FILTER, reads the CSRF from the session and exposes its information in
 * response headers
 * 
 * @author i031908
 */
@Named("csrfHeadersFilter")
public class CsrfHeadersFilter extends OncePerRequestFilter {

    private static final String SPRING_SECURITY_CSRF_SESSION_ATTRIBUTE = "_csrf";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        CsrfToken token = (CsrfToken) request.getAttribute(SPRING_SECURITY_CSRF_SESSION_ATTRIBUTE);
        if (token != null) {
            response.setHeader(Constants.CSRF_HEADER_NAME, token.getHeaderName());
            response.setHeader(Constants.CSRF_PARAM_NAME, token.getParameterName());
            response.setHeader(Constants.CSRF_TOKEN, token.getToken());
        }
        filterChain.doFilter(request, response);
    }

}
