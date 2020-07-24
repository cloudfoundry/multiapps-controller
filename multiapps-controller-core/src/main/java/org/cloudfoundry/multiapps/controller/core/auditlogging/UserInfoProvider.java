package org.cloudfoundry.multiapps.controller.core.auditlogging;

import org.cloudfoundry.multiapps.controller.core.util.UserInfo;

public interface UserInfoProvider {
    UserInfo getUserInfo();
}