package com.sap.cloud.lm.sl.cf.client;

import org.springframework.security.oauth2.common.OAuth2AccessToken;

public interface TokenProvider {

    OAuth2AccessToken getToken();

}
