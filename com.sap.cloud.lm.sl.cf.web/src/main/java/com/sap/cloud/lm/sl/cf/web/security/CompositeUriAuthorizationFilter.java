package com.sap.cloud.lm.sl.cf.web.security;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named("compositeUriAuthorizationFilter")
public class CompositeUriAuthorizationFilter extends AuthorizationFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeUriAuthorizationFilter.class);

    private final List<UriAuthorizationFilter> uriAuthorizationFilters;

    @Inject
    public CompositeUriAuthorizationFilter(List<UriAuthorizationFilter> uriAuthorizationFilters) {
        this.uriAuthorizationFilters = uriAuthorizationFilters;
    }

    @Override
    protected void ensureUserIsAuthorized(HttpCommunication communication) throws IOException {
        try {
            String uri = getRequestUri(communication);
            LOGGER.trace("Looking for a matching authorization filter for request to \"{}\"...", uri);
            LOGGER.trace("Registered authorization filters: {}", uriAuthorizationFilters);
            for (UriAuthorizationFilter uriAuthorizationFilter : uriAuthorizationFilters) {
                if (uri.matches(uriAuthorizationFilter.getUriRegex())) {
                    ensureUserIsAuthorized(uriAuthorizationFilter, communication);
                    return;
                }
            }
        } catch (AuthorizationException e) {
            communication.getResponse()
                         .sendError(e.getStatusCode(), e.getMessage());
        }
    }

    private void ensureUserIsAuthorized(UriAuthorizationFilter uriAuthorizationFilter, HttpCommunication communication) throws IOException {
        LOGGER.debug("Using authorization filter {} for request to \"{}\".", uriAuthorizationFilter, getRequestUri(communication));
        uriAuthorizationFilter.ensureUserIsAuthorized(communication);
    }

    private String getRequestUri(HttpCommunication communication) {
        return communication.getRequest()
                            .getRequestURI();
    }

}
