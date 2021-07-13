package org.cloudfoundry.multiapps.controller.core.security.token.parsers;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

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
        OAuth2AccessTokenWithAdditionalInfo parsedToken = parseTokenString(tokenString);
        logTokenInfo(parsedToken.getOAuth2AccessToken());
        return parsedToken;
    }

    private OAuth2AccessTokenWithAdditionalInfo parseTokenString(String tokenString) {
        return tokenParsers.stream()
                           .map(tokenParser -> tokenParser.parse(tokenString))
                           .filter(Optional::isPresent)
                           .map(Optional::get)
                           .findFirst()
                           .orElseThrow(() -> new InternalAuthenticationServiceException(Messages.NO_TOKEN_PARSER_FOUND_FOR_THE_CURRENT_TOKEN));
    }

    private void logTokenInfo(OAuth2AccessToken accessToken) {
        LOGGER.debug("Parsed token value: {0}", accessToken.getTokenValue());
        LOGGER.debug("Parsed token type: {0}", accessToken.getTokenType());
        LOGGER.debug("Parsed token expires in: {0}", accessToken.getExpiresAt());
    }

}
