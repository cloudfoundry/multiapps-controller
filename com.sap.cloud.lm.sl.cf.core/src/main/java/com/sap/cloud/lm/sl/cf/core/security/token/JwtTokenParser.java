package com.sap.cloud.lm.sl.cf.core.security.token;

import javax.inject.Inject;

import org.springframework.core.annotation.Order;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.util.TokenFactory;

@Component
@Order(0)
public class JwtTokenParser implements TokenParser {

    private final TokenFactory tokenFactory;

    @Inject
    public JwtTokenParser(TokenFactory tokenFactory) {
        this.tokenFactory = tokenFactory;
    }

    @Override
    public OAuth2AccessToken parse(String tokenString) {
        return tokenFactory.createToken(tokenString);
    }
}
