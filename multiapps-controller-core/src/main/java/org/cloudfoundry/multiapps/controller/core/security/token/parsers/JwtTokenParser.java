package org.cloudfoundry.multiapps.controller.core.security.token.parsers;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.uaa.UAAClient;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.security.token.parsing.TokenValidationStrategyFactory;
import org.cloudfoundry.multiapps.controller.core.security.token.parsing.ValidationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.InternalAuthenticationServiceException;

import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import com.sap.cloudfoundry.client.facade.oauth2.TokenFactory;

@Named
@Order(0)
public class JwtTokenParser implements TokenParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtTokenParser.class);

    protected final TokenFactory tokenFactory;
    private TokenKey tokenKey;
    private final UAAClient uaaClient;
    private final TokenValidationStrategyFactory tokenValidationStrategyFactory;

    @Inject
    public JwtTokenParser(UAAClient uaaClient, TokenValidationStrategyFactory tokenValidationStrategyFactory) {
        this.tokenFactory = new TokenFactory();
        this.uaaClient = uaaClient;
        this.tokenValidationStrategyFactory = tokenValidationStrategyFactory;
    }

    @Override
    public Optional<OAuth2AccessTokenWithAdditionalInfo> parse(String tokenString) {
        try {
            verifyToken(tokenString);
            return Optional.of(tokenFactory.createToken(tokenString));
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return Optional.empty();
        }
    }

    protected void verifyToken(String tokenString) {
        try {
            verify(tokenString);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            refreshTokenKey();
            verify(tokenString);
        }
    }

    private void verify(String tokenString) {
        TokenKey tokenKey = getCachedTokenKey();
        String algorithm = tokenKey.getAlgorithm();
        ValidationStrategy validationStrategy = tokenValidationStrategyFactory.createStrategy(algorithm);
        if (!validationStrategy.validateToken(tokenString, tokenKey.getValue())) {
            throw new InternalAuthenticationServiceException(Messages.INVALID_TOKEN_PROVIDED);
        }
    }

    private TokenKey getCachedTokenKey() {
        if (tokenKey == null) {
            synchronized (this) {
                if (tokenKey == null) {
                    refreshTokenKey();
                }
            }
        }
        return tokenKey;
    }

    private void refreshTokenKey() {
        tokenKey = readTokenKey();
    }

    private TokenKey readTokenKey() {
        Map<String, Object> tokenKeyResponse = uaaClient.readTokenKey();
        Object value = tokenKeyResponse.get("value");
        Object algorithm = tokenKeyResponse.get("alg");
        if (value == null || algorithm == null) {
            throw new InternalAuthenticationServiceException("Response from /token_key does not contain a key value or an algorithm");
        }
        return new TokenKey(value.toString(), algorithm.toString());
    }

    static class TokenKey {

        private final String value;
        private final String algorithm;

        TokenKey(String value, String algorithm) {
            this.value = value;
            this.algorithm = algorithm;
        }

        String getValue() {
            return value;
        }

        String getAlgorithm() {
            return algorithm;
        }

    }
}
