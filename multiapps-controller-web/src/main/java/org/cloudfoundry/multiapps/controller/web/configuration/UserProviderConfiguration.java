package org.cloudfoundry.multiapps.controller.web.configuration;

import org.cloudfoundry.multiapps.controller.core.auditlogging.UserInfoProvider;
import org.cloudfoundry.multiapps.controller.web.util.SecurityContextUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserProviderConfiguration {

    @Bean
    public UserInfoProvider buildUserInfoProvider() {
        return SecurityContextUtil::getUserInfo;
    }
}
