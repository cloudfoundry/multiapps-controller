package com.sap.cloud.lm.sl.cf.core.security.token;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.InvalidSignatureException;
import org.springframework.security.jwt.crypto.sign.MacSigner;
import org.springframework.security.jwt.crypto.sign.RsaVerifier;
import org.springframework.security.jwt.crypto.sign.SignatureVerifier;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;

import com.sap.cloud.lm.sl.cf.client.uaa.UAAClient;
import com.sap.cloud.lm.sl.cf.client.util.TokenFactory;

@Named
@Order(0)
public class JwtTokenParser implements TokenParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtTokenParser.class);

    protected final TokenFactory tokenFactory;
    private TokenKey tokenKey;
    private final UAAClient uaaClient;

    @Inject
    public JwtTokenParser(TokenFactory tokenFactory, UAAClient uaaClient) {
        this.tokenFactory = tokenFactory;
        this.uaaClient = uaaClient;
    }

    @Override
    public OAuth2AccessToken parse(String tokenString) {
        try {
            verifyToken(tokenString);

            return tokenFactory.createToken(tokenString);
        } catch (IllegalStateException e) {
            LOGGER.debug("Error parsing jwt token", e);
            return null;
        }
    }

    protected void verifyToken(String tokenString) {
        try {
            decodeAndVerify(tokenString);
        } catch (InvalidTokenException e) {
            refreshTokenKey();
            decodeAndVerify(tokenString);
        }

    }

    private void decodeAndVerify(String tokenString) {
        try {
            JwtHelper.decodeAndVerify(tokenString, getSignatureVerifier(getCachedTokenKey()));
        } catch (InvalidSignatureException e) {
            throw new InvalidTokenException(e.getMessage(), e);
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

    private static SignatureVerifier getSignatureVerifier(TokenKey tokenKey) {
        String alg = tokenKey.getAlgorithm();
        SignatureVerifier verifier;
        // TODO: Find or implement a factory, which would support other algorithms like SHA384withRSA, SHA512withRSA and HmacSHA512.
        if (alg.equals("SHA256withRSA") || alg.equals("RS256"))
            verifier = new RsaVerifier(tokenKey.getValue());
        else if (alg.equals("HMACSHA256") || alg.equals("HS256"))
            verifier = new MacSigner(tokenKey.getValue());
        else
            throw new InternalAuthenticationServiceException("Unsupported verifier algorithm " + alg);
        return verifier;
    }

    private TokenKey readTokenKey() {
        Map<String, Object> tokenKeyResponse = uaaClient.readTokenKey();
        Object value = tokenKeyResponse.get("value");
        Object alg = tokenKeyResponse.get("alg");
        if (value == null || alg == null) {
            throw new InternalAuthenticationServiceException("Response from /token_key does not contain a key value or an algorithm");
        }
        return new TokenKey(value.toString(), alg.toString());

    }

    private static class TokenKey {
        private String value;
        private String algorithm;

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
