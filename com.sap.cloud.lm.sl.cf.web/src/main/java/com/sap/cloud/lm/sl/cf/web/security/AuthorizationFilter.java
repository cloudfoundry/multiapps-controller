package com.sap.cloud.lm.sl.cf.web.security;

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
        ensureUserIsAuthorized(ImmutableHttpCommunication.of(request, response));
        filterChain.doFilter(request, response);
    }

    protected abstract void ensureUserIsAuthorized(HttpCommunication communication) throws IOException;

}
