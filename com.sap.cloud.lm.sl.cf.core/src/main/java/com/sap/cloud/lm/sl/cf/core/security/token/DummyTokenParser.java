package com.sap.cloud.lm.sl.cf.core.security.token;

import javax.inject.Inject;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.util.TokenFactory;
import com.sap.cloud.lm.sl.cf.core.util.Configuration;
import com.sap.cloud.lm.sl.cf.core.util.SecurityUtil;

@Component
@Conditional(DummyTokenParser.DummyTokenParserCondition.class)
@Order(Ordered.LOWEST_PRECEDENCE)
public class DummyTokenParser implements TokenParser {

    private final TokenFactory tokenFactory;

    @Inject
    public DummyTokenParser(TokenFactory tokenFactory) {
        this.tokenFactory = tokenFactory;
    }

    @Override
    public OAuth2AccessToken parse(String tokenString) {
        if (tokenString.equals(TokenFactory.DUMMY_TOKEN)) {
            return tokenFactory.createDummyToken("dummy", SecurityUtil.CLIENT_ID);
        }
        return null;
    }

    public static class DummyTokenParserCondition implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            Configuration configuration = Configuration.getInstance();
            return configuration.areDummyTokensEnabled();
        }

    }

}
