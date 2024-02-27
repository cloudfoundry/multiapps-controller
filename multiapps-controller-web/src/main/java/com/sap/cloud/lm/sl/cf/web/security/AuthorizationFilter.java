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

@Named("authorizationFilter")
public class AuthorizationFilter extends OncePerRequestFilter {

    private static final String SPACE_ID_REGEX = "/api/v1/spaces/(.*?)/.*";

    @Inject
    private AuthorizationChecker authorizationChecker;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        String spaceId = getSpaceIdFromUri(requestUri);

        try {
            // The spaceId will be null when the requestURI does not match the expected one /api/v1/spaces/*.
            // This could happen when the filter is used for processing requests from the SLP API.
            // In such cases the filter will be skipped.
            if (spaceId != null) {
                UserInfo userInfo = SecurityContextUtil.getUserInfo();
                authorizationChecker.ensureUserIsAuthorized(request, userInfo, spaceId, null);
            }
        } catch (WebApplicationException e) {
            response.sendError(401, MessageFormat.format(Messages.NOT_AUTHORIZED_TO_PERFORM_OPERATIONS_IN_SPACE, spaceId));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getSpaceIdFromUri(String requestUri) {
        Pattern pattern = Pattern.compile(SPACE_ID_REGEX);
        Matcher regexMatcher = pattern.matcher(requestUri);
        if (regexMatcher.matches()) {
            return regexMatcher.group(1);
        }
        return null;
    }

}
