package com.sap.cloud.lm.sl.cf.core.security.token;

import java.text.MessageFormat;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

@Named
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
                LOGGER.debug(MessageFormat.format("Parsed token value: {0}", parsedToken.getValue()));
                LOGGER.debug(MessageFormat.format("Parsed token type: {0}", parsedToken.getTokenType()));
                LOGGER.debug(MessageFormat.format("Parsed token expires in: {0}", parsedToken.getExpiresIn()));
                return parsedToken;
            }
        }
        return null;
    }

}
