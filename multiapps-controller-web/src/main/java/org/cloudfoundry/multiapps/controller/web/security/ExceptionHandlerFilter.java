package org.cloudfoundry.multiapps.controller.web.security;

import java.io.IOException;

import javax.inject.Named;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

@Named
public class ExceptionHandlerFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandlerFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (ResponseStatusException e) {
            LOGGER.error(e.getMessage(), e);
            response.sendError(e.getStatus()
                                .value());
            response.getWriter()
                    .write(e.getMessage());
        } catch (InsufficientAuthenticationException e) {
            LOGGER.error(e.getMessage(), e);
            response.sendError(HttpStatus.SC_FORBIDDEN);
        } catch (InternalAuthenticationServiceException e) {
            LOGGER.error(e.getMessage(), e);
            response.sendError(HttpStatus.SC_UNAUTHORIZED);
        }
    }
}
