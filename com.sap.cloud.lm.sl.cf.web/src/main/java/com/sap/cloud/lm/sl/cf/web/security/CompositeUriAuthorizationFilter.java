package com.sap.cloud.lm.sl.cf.web.security;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.web.util.ServletUtils;

@Named("compositeUriAuthorizationFilter")
public class CompositeUriAuthorizationFilter extends AuthorizationFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeUriAuthorizationFilter.class);

    private final List<UriAuthorizationFilter> uriAuthorizationFilters;

    @Inject
    public CompositeUriAuthorizationFilter(List<UriAuthorizationFilter> uriAuthorizationFilters) {
        this.uriAuthorizationFilters = uriAuthorizationFilters;
    }

    @Override
    protected boolean ensureUserIsAuthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String uri = ServletUtils.getDecodedURI(request);
            LOGGER.trace("Looking for a matching authorization filter for request to \"{}\"...", uri);
            LOGGER.trace("Registered authorization filters: {}", uriAuthorizationFilters);
            for (UriAuthorizationFilter uriAuthorizationFilter : uriAuthorizationFilters) {
                if (uri.matches(uriAuthorizationFilter.getUriRegex())) {
                    return ensureUserIsAuthorized(uriAuthorizationFilter, request, response);
                }
            }
            LOGGER.trace("No matching authorization filter for request to \"{}\".", uri);
            return true;
        } catch (AuthorizationException e) {
            response.sendError(e.getStatusCode(), e.getMessage());
            return false;
        }
    }

    private boolean ensureUserIsAuthorized(UriAuthorizationFilter uriAuthorizationFilter, HttpServletRequest request,
                                           HttpServletResponse response)
        throws IOException {
        LOGGER.debug("Using authorization filter {} for request to \"{}\".", uriAuthorizationFilter, ServletUtils.getDecodedURI(request));
        return uriAuthorizationFilter.ensureUserIsAuthorized(request, response);
    }

}
