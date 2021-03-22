package org.cloudfoundry.multiapps.controller.core.security.token.parsers;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.uaa.UAAClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.MacSigner;
import org.springframework.security.jwt.crypto.sign.RsaVerifier;
import org.springframework.security.jwt.crypto.sign.SignatureVerifier;

import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import com.sap.cloudfoundry.client.facade.oauth2.TokenFactory;

@Named
@Order(0)
public class JwtTokenParser implements TokenParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtTokenParser.class);

    protected final TokenFactory tokenFactory;
    private TokenKey tokenKey;
    private final UAAClient uaaClient;

    @Inject
    public JwtTokenParser(UAAClient uaaClient) {
        this.tokenFactory = new TokenFactory();
        this.uaaClient = uaaClient;
    }

    @Override
    public OAuth2AccessTokenWithAdditionalInfo parse(String tokenString) {
        verifyToken(tokenString);
        return tokenFactory.createToken(tokenString);
    }

    protected void verifyToken(String tokenString) {
        try {
            decodeAndVerify(tokenString);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            refreshTokenKey();
            decodeAndVerify(tokenString);
        }
    }

    private void decodeAndVerify(String tokenString) {
        try {
            JwtHelper.decodeAndVerify(tokenString, getSignatureVerifier(getCachedTokenKey()));
        } catch (Exception e) {
            throw new InternalAuthenticationServiceException(e.getMessage(), e);
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
