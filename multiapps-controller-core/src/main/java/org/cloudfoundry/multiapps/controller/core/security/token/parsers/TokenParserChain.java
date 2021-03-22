package org.cloudfoundry.multiapps.controller.core.security.token.parsers;

import java.text.MessageFormat;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;

@Named
public class TokenParserChain {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenParserChain.class);

    private final List<TokenParser> tokenParsers;

    @Inject
    public TokenParserChain(List<TokenParser> tokenParsers) {
        LOGGER.debug(MessageFormat.format("Parser chain: {0}", tokenParsers));
        this.tokenParsers = tokenParsers;
    }

    public OAuth2AccessTokenWithAdditionalInfo parse(String tokenString) {
        for (TokenParser tokenParser : tokenParsers) {
            OAuth2AccessTokenWithAdditionalInfo parsedToken = tokenParser.parse(tokenString);
            if (parsedToken != null) {
                LOGGER.debug(MessageFormat.format("Parsed token value: {0}", parsedToken.getOAuth2AccessToken()
                                                                                        .getTokenValue()));
                LOGGER.debug(MessageFormat.format("Parsed token type: {0}", parsedToken.getOAuth2AccessToken()
                                                                                       .getTokenType()));
                LOGGER.debug(MessageFormat.format("Parsed token expires in: {0}", parsedToken.getOAuth2AccessToken()
                                                                                             .getExpiresAt()));
                return parsedToken;
            }
        }
        return null;
    }

}
