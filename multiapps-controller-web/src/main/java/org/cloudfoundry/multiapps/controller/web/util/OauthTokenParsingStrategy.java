package org.cloudfoundry.multiapps.controller.web.util;

import org.cloudfoundry.multiapps.controller.core.security.token.parsers.TokenParserChain;

import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;

public class OauthTokenParsingStrategy implements TokenParsingStrategy {

    private final TokenParserChain tokenParserChain;

    public OauthTokenParsingStrategy(TokenParserChain tokenParserChain) {
        this.tokenParserChain = tokenParserChain;
    }

    @Override
    public OAuth2AccessTokenWithAdditionalInfo parseToken(String tokenString) {
        return tokenParserChain.parse(tokenString);
    }
}
