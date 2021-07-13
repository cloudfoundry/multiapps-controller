package org.cloudfoundry.multiapps.controller.core.security.token.parsers;

import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;

import java.util.Optional;

public interface TokenParser {

    Optional<OAuth2AccessTokenWithAdditionalInfo> parse(String tokenString);

}
