package com.sap.cloud.lm.sl.cf.core.security.token;

import org.springframework.security.oauth2.common.OAuth2AccessToken;

public interface TokenParser {

    OAuth2AccessToken parse(String tokenString);

}
