package com.sap.cloud.lm.sl.cf.core.auditlogging;

import com.sap.cloud.lm.sl.cf.core.util.UserInfo;

public interface UserInfoProvider {
    UserInfo getUserInfo();
}