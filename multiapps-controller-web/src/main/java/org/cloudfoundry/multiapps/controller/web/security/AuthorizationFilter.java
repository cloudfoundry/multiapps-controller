package org.cloudfoundry.multiapps.controller.web.security;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

public abstract class AuthorizationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        if (!ensureUserIsAuthorized(request, response)) {
            return;
        }
        filterChain.doFilter(request, response);
    }

    protected void doFilterInternalFake(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        if (!ensureUserIsAuthorized(request, response)) {
            return;
        }
        filterChain.doFilter(request, response);
    }
    
    /**
     * @return Whether or not the request should be forwarded to the rest of the filter chain and eventually to the appropriate handler.
     */
    protected abstract boolean ensureUserIsAuthorized(HttpServletRequest request, HttpServletResponse response) throws IOException;

}
