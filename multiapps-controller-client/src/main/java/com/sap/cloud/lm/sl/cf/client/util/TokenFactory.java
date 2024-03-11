package com.sap.cloud.lm.sl.cf.client.util;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Component
public class TokenFactory {

    // Scopes:
    public static final String SCOPE_CC_READ = "cloud_controller.read";
    public static final String SCOPE_CC_WRITE = "cloud_controller.write";
    public static final String SCOPE_CC_ADMIN = "cloud_controller.admin";
    public static final String SCOPE_OPENID = "openid";

    // Token Body elements:
    public static final String SCOPE = "scope";
    public static final String CLIENT_ID = "client_id";
    public static final String EXPIRES_AT_KEY = "exp";
    public static final String USER_NAME = "username";
    public static final String ISSUED_AT_KEY = "iat";

    public OAuth2AccessTokenWithAdditionalInfo createToken(String tokenString) {
        Map<String, Object> tokenInfo = parseToken(tokenString);
        return createToken(tokenString, tokenInfo);
    }

    @SuppressWarnings("unchecked")
    public OAuth2AccessTokenWithAdditionalInfo createToken(String tokenString, Map<String, Object> tokenInfo) {
        List<String> scope = (List<String>) tokenInfo.get(SCOPE);
        Number expiresAt = (Number) tokenInfo.get(EXPIRES_AT_KEY);
        Number instantiatedAt = (Number) tokenInfo.get(ISSUED_AT_KEY);
        List<String> requiredElements = Arrays.asList(SCOPE, EXPIRES_AT_KEY, ISSUED_AT_KEY);
        if (scope == null || expiresAt == null || instantiatedAt == null) {
            throw new IllegalStateException(MessageFormat.format("One or more of the following elements are missing from the token: \"{0}\"",
                                                                 requiredElements));
        }
        return new OAuth2AccessTokenWithAdditionalInfo(createOAuth2AccessToken(tokenString, scope, expiresAt, instantiatedAt), tokenInfo);
    }

    private OAuth2AccessToken createOAuth2AccessToken(String tokenString, List<String> scope, Number expiresAt, Number instantiatedAt) {
        try {
            return new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                                         tokenString,
                                         Instant.ofEpochSecond(instantiatedAt.longValue()),
                                         Instant.ofEpochSecond(expiresAt.longValue()),
                                         new HashSet<>(scope));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage(), e);
        }
    }

    private Map<String, Object> parseToken(String tokenString) {
        String[] tokenParts = tokenString.split("\\.");
        if (tokenParts.length != 3) {
            // The token should have three parts (header, body and signature) separated by a dot. It doesn't, so we consider it as invalid.
            return Collections.emptyMap();
        }
        String body = decode(tokenParts[1]);
        return JsonUtil.convertJsonToMap(body);
    }

    private String decode(String string) {
        Base64.Decoder decoder = Base64.getUrlDecoder();
        return new String(decoder.decode(string), StandardCharsets.UTF_8);
    }

}
