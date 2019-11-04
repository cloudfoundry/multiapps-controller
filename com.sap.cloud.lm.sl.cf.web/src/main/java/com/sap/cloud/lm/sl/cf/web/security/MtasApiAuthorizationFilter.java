package com.sap.cloud.lm.sl.cf.web.security;

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.web.util.ServletUtils;

@Named
public class MtasApiAuthorizationFilter extends SpaceGuidBasedAuthorizationFilter {

    private static final String SPACE_GUID_CAPTURING_REGEX = "/api/v1/spaces/(.*?)/.*";

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
        String uri = ServletUtils.getDecodedURI(request);
        return extractSpaceGuid(uri);
    }

    private String extractSpaceGuid(String uri) {
        Pattern pattern = Pattern.compile(SPACE_GUID_CAPTURING_REGEX);
        Matcher matcher = pattern.matcher(uri);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        throw new AuthorizationException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                         MessageFormat.format("Could not extract space GUID from URI \"{0}\".", uri));
    }

}
