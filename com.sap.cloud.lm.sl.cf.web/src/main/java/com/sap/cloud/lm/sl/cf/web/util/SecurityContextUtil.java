package com.sap.cloud.lm.sl.cf.web.util;

import static com.sap.cloud.lm.sl.cf.core.util.SecurityUtil.USER_INFO;

import java.security.Principal;

import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import com.sap.cloud.lm.sl.cf.core.util.UserInfo;

public final class SecurityContextUtil {

    private SecurityContextUtil() {
    }

    public static UserInfo getUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext()
                                                             .getAuthentication();
        if (authentication == null) {
            throw new InternalAuthenticationServiceException("Not logged in!");
        }
        OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) authentication;
        return (UserInfo) oAuth2AuthenticationToken.getPrincipal()
                                                   .getAttributes()
                                                   .get(USER_INFO);
    }

    public static String getUsername(Principal principal) {
        if (principal instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) principal;
            return ((UserInfo) oAuth2AuthenticationToken.getPrincipal()
                                                        .getAttributes()
                                                        .get(USER_INFO)).getName();
        }
        return principal.getName();
    }

}
