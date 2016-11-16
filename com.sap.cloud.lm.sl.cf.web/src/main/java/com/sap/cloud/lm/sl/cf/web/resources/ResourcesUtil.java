package com.sap.cloud.lm.sl.cf.web.resources;

import org.activiti.engine.IdentityService;
import org.activiti.engine.identity.User;

public class ResourcesUtil {

    static void persistUser(IdentityService identityService, String userId) {
        User user = identityService.createUserQuery().userId(userId).singleResult();
        if (user == null) {
            user = identityService.newUser(userId);
            identityService.saveUser(user);
        }
    }

}
