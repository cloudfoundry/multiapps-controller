package com.sap.cloud.lm.sl.cf.core.security.token;

import javax.inject.Inject;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.util.TokenFactory;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.core.util.SecurityUtil;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class DummyTokenParser implements TokenParser {

    private final TokenFactory tokenFactory;
    private final ApplicationConfiguration applicationConfiguration;

    @Inject
    public DummyTokenParser(TokenFactory tokenFactory, ApplicationConfiguration applicationConfiguration) {
        this.tokenFactory = tokenFactory;
        this.applicationConfiguration = applicationConfiguration;
    }

    @Override
    public OAuth2AccessToken parse(String tokenString) {
        if (applicationConfiguration.areDummyTokensEnabled() && tokenString.equals(TokenFactory.DUMMY_TOKEN)) {
            return tokenFactory.createDummyToken("dummy", SecurityUtil.CLIENT_ID);
        }
        return null;
    }

}
