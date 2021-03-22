package org.cloudfoundry.multiapps.controller.web.util;

import static org.cloudfoundry.multiapps.controller.core.util.SecurityUtil.USER_INFO;

import java.security.Principal;

import org.cloudfoundry.multiapps.controller.core.util.UserInfo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

public class SecurityContextUtil {

    private SecurityContextUtil() {
    }

    public static UserInfo getUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext()
                                                             .getAuthentication();
        if (authentication == null) {
            return null;
        }
        OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) authentication;
        return (UserInfo) oAuth2AuthenticationToken.getPrincipal()
                                                   .getAttributes()
                                                   .get(USER_INFO);
    }

    public static String getUsername() {
        UserInfo userInfo = getUserInfo();
        return userInfo == null ? null : userInfo.getName();
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
