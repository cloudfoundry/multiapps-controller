package org.cloudfoundry.multiapps.controller.web.security;

import java.io.IOException;

import javax.inject.Named;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cloudfoundry.multiapps.controller.web.Constants;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

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
        filterChain.doFilter(request, response);
        // Do not move filterChain.doFilter after the header copy!
        // The order is deliberately set to copy the csrf token on the way back, after the whole filter chain has passed.
        // This is because user authentication may force session & token recreation after this filter.
        CsrfToken token = (CsrfToken) request.getAttribute(SPRING_SECURITY_CSRF_SESSION_ATTRIBUTE);
        if (token != null && !response.isCommitted()) { // Spring invokes HttpServletResponse#sendError in case of invalid
                                                        // credentials, which commits the response and should not be modified
            response.setHeader(Constants.CSRF_HEADER_NAME, token.getHeaderName());
            response.setHeader(Constants.CSRF_PARAM_NAME, token.getParameterName());
            response.setHeader(Constants.CSRF_TOKEN, token.getToken());
        }
    }

}
