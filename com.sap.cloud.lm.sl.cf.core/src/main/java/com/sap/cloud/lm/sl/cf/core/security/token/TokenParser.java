package com.sap.cloud.lm.sl.cf.core.security.token;

import java.util.Optional;

import org.cloudfoundry.client.lib.oauth2.OAuth2AccessTokenWithAdditionalInfo;

public interface TokenParser {

    Optional<OAuth2AccessTokenWithAdditionalInfo> parse(String tokenString);

}
