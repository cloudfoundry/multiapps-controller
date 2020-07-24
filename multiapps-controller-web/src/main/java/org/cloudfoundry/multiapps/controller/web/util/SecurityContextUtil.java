package org.cloudfoundry.multiapps.controller.web.util;

import org.cloudfoundry.multiapps.controller.core.util.UserInfo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityContextUtil {

    private SecurityContextUtil() {
    }

    public static UserInfo getUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext()
                                                             .getAuthentication();
        return authentication == null ? null : (UserInfo) authentication.getPrincipal();
    }

    public static String getUserName() {
        UserInfo userInfo = getUserInfo();
        return userInfo == null ? null : userInfo.getName();
    }

}
