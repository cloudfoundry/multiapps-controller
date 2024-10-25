package org.cloudfoundry.multiapps.controller.web.security;

import java.io.IOException;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.cloudfoundry.multiapps.controller.web.resources.CFExceptionMapper;
import org.cloudfoundry.multiapps.controller.web.util.ServletUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

@Named("compositeUriAuthorizationFilter")
public class CompositeUriAuthorizationFilter extends AuthorizationFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeUriAuthorizationFilter.class);

    private final List<UriAuthorizationFilter> uriAuthorizationFilters;
    private final CFExceptionMapper exceptionMapper;

    @Inject
    public CompositeUriAuthorizationFilter(List<UriAuthorizationFilter> uriAuthorizationFilters, CFExceptionMapper exceptionMapper) {
        this.uriAuthorizationFilters = uriAuthorizationFilters;
        this.exceptionMapper = exceptionMapper;
    }

    @Override
    protected boolean ensureUserIsAuthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String uri = ServletUtil.decodeUri(request);
            String normalizedUri = ServletUtil.removeInvalidForwardSlashes(uri);
            LOGGER.trace("Looking for a matching authorization filter for request to \"{}\"...", uri);
            LOGGER.trace("Registered authorization filters: {}", uriAuthorizationFilters);
            for (UriAuthorizationFilter uriAuthorizationFilter : uriAuthorizationFilters) {
                if (normalizedUri.matches(uriAuthorizationFilter.getUriRegex())) {
                    return ensureUserIsAuthorized(uriAuthorizationFilter, request, response);
                }
            }
            LOGGER.trace("No matching authorization filter for request to \"{}\".", uri);
            return true;
        } catch (Exception e) {
            ResponseEntity<String> responseEntity = exceptionMapper.handleException(e);
            ServletUtil.send(response, responseEntity.getStatusCodeValue(), responseEntity.getBody());
            return false;
        }
    }

    private boolean ensureUserIsAuthorized(UriAuthorizationFilter uriAuthorizationFilter, HttpServletRequest request,
                                           HttpServletResponse response)
        throws IOException {
        LOGGER.debug("Using authorization filter {} for request to \"{}\".", uriAuthorizationFilter, ServletUtil.decodeUri(request));
        return uriAuthorizationFilter.ensureUserIsAuthorized(request, response);
    }

}
