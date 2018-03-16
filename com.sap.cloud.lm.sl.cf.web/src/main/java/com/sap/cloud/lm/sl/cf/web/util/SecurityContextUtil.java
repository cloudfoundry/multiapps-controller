package com.sap.cloud.lm.sl.cf.web.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.sap.cloud.lm.sl.cf.core.util.UserInfo;

public class SecurityContextUtil {

    public static UserInfo getUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext()
            .getAuthentication();
        return authentication == null ? null : (UserInfo) authentication.getPrincipal();
    }

}
