package org.cloudfoundry.multiapps.controller.web.security;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.web.util.ServletUtil;

@Named
public class DefaultSpaceGuidBasedAuthorizationFilter extends SpaceGuidBasedAuthorizationFilter {

    private static final String SPACE_GUID_CAPTURING_REGEX = "/api/v\\d+/spaces/(.*?)/.*";
    private static final Pattern DEFAULT_URI_PATTERN = Pattern.compile(SPACE_GUID_CAPTURING_REGEX);

    @Inject
    public DefaultSpaceGuidBasedAuthorizationFilter(AuthorizationChecker authorizationChecker) {
        super(authorizationChecker);
    }

    @Override
    public String getUriRegex() {
        return SPACE_GUID_CAPTURING_REGEX;
    }

    protected Pattern getUriPattern() {
        return DEFAULT_URI_PATTERN;
    }

    @Override
    protected String extractSpaceGuid(HttpServletRequest request) {
        String uri = ServletUtil.decodeUri(request);
        return extractSpaceGuid(ServletUtil.removeInvalidForwardSlashes(uri));
    }

    private String extractSpaceGuid(String uri) {
        Matcher matcher = getUriPattern().matcher(uri);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        throw new SLException("Could not extract space GUID from URI \"{0}\".", uri);
    }

}
