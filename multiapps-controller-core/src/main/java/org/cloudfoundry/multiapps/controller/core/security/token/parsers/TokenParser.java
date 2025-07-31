package org.cloudfoundry.multiapps.controller.core.security.token.parsers;

import java.util.Optional;

import org.cloudfoundry.multiapps.controller.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;

public interface TokenParser {

    Optional<OAuth2AccessTokenWithAdditionalInfo> parse(String tokenString);

}
