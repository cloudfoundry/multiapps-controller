package com.sap.cloud.lm.sl.cf.web.security;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import com.sap.cloud.lm.sl.cf.web.util.ServletUtil;
import com.sap.cloud.lm.sl.common.SLException;

@Named
public class MtasApiAuthorizationFilter extends SpaceGuidBasedAuthorizationFilter {

    private static final String SPACE_GUID_CAPTURING_REGEX = "/api/v\\d+/spaces/(.*?)/.*";

    @Inject
    public MtasApiAuthorizationFilter(AuthorizationChecker authorizationChecker) {
        super(authorizationChecker);
    }

    @Override
    public String getUriRegex() {
        return SPACE_GUID_CAPTURING_REGEX;
    }

    @Override
    protected String extractSpaceGuid(HttpServletRequest request) {
        String uri = ServletUtil.decodeUri(request);
        return extractSpaceGuid(ServletUtil.removeInvalidForwardSlashes(uri));
    }

    private String extractSpaceGuid(String uri) {
        Pattern pattern = Pattern.compile(SPACE_GUID_CAPTURING_REGEX);
        Matcher matcher = pattern.matcher(uri);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        throw new SLException("Could not extract space GUID from URI \"{0}\".", uri);
    }

}
