package com.sap.cloud.lm.sl.cf.core.security.token;

import java.text.MessageFormat;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Component;

@Component
public class TokenParserChain {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenParserChain.class);

    private final List<TokenParser> tokenParsers;

    @Inject
    public TokenParserChain(List<TokenParser> tokenParsers) {
        LOGGER.debug(MessageFormat.format("Parser chain: {0}", tokenParsers));
        this.tokenParsers = tokenParsers;
    }

    public OAuth2AccessToken parse(String tokenString) {
        for (TokenParser tokenParser : tokenParsers) {
            OAuth2AccessToken parsedToken = tokenParser.parse(tokenString);
            if (parsedToken != null) {
                LOGGER.debug("Parsed token value: " + parsedToken.getValue());
                LOGGER.debug("Parsed token type: " + parsedToken.getTokenType());
                LOGGER.debug("Parsed token expires in: " + parsedToken.getExpiresIn());
                return parsedToken;
            }
        }
        return null;
    }

}
