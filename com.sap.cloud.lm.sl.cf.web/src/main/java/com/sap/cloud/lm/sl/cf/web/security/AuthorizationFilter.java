package com.sap.cloud.lm.sl.cf.web.security;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;

import org.springframework.web.filter.OncePerRequestFilter;

import com.sap.cloud.lm.sl.cf.core.util.UserInfo;
import com.sap.cloud.lm.sl.cf.web.message.Messages;
import com.sap.cloud.lm.sl.cf.web.util.SecurityContextUtil;

@Named("authFilter")
public class AuthorizationFilter extends OncePerRequestFilter {

    private static final String SPACE_GUID_REGEX = "/api/v1/spaces/(.*?)/.*";

    @Inject
    private AuthorizationChecker authorizationChecker;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String uri = request.getRequestURI();
        String spaceGuid = getSpaceGuidFromUri(uri);

        try {
            // The space GUID will be null when the URI does not match the SPACE_GUID_REGEX. This could happen when the filter is used for
            // processing of requests to the SLP API. In such cases this filter should be skipped.
            if (spaceGuid != null) {
                UserInfo userInfo = SecurityContextUtil.getUserInfo();
                authorizationChecker.ensureUserIsAuthorized(request, userInfo, spaceGuid, null);
            }
        } catch (WebApplicationException e) {
            response.sendError(401, MessageFormat.format(Messages.NOT_AUTHORIZED_TO_PERFORM_OPERATIONS_IN_SPACE, spaceGuid));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getSpaceGuidFromUri(String uri) {
        Pattern pattern = Pattern.compile(SPACE_GUID_REGEX);
        Matcher matcher = pattern.matcher(uri);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

}
