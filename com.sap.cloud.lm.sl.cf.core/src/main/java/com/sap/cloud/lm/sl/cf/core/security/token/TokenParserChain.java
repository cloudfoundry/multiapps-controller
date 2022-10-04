package com.sap.cloud.lm.sl.cf.core.security.token;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.message.Messages;

@Component
public class TokenParserChain {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenParserChain.class);

    private final List<TokenParser> tokenParsers;

    @Inject
    public TokenParserChain(List<TokenParser> tokenParsers) {
        LOGGER.debug("Parser chain: " + tokenParsers.toString());
        this.tokenParsers = tokenParsers;
    }

    public OAuth2AccessTokenWithAdditionalInfo parse(String tokenString) {
        OAuth2AccessTokenWithAdditionalInfo parsedToken = parseTokenString(tokenString);
        logTokenInfo(parsedToken);
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

    private void logTokenInfo(OAuth2AccessTokenWithAdditionalInfo accessToken) {
        LOGGER.debug(MessageFormat.format(Messages.PARSED_TOKEN_TYPE_0, accessToken.getType()));
        LOGGER.debug(MessageFormat.format(Messages.PARSED_TOKEN_EXPIRES_IN_0, accessToken.getExpiresAt()));
    }

}
