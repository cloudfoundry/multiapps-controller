package org.cloudfoundry.multiapps.controller.client;

import org.springframework.security.oauth2.common.OAuth2AccessToken;

public interface TokenProvider {

    OAuth2AccessToken getToken();

}
