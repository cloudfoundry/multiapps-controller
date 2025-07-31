package org.cloudfoundry.multiapps.controller.client.facade.oauth2;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.server.ResponseStatusException;

import org.cloudfoundry.multiapps.controller.client.facade.util.JsonUtil;

public class TokenFactory {

    private static final int JWT_TOKEN_PARTS_COUNT = 3;

    // Scopes:
    public static final String SCOPE_CC_READ = "cloud_controller.read";
    public static final String SCOPE_CC_WRITE = "cloud_controller.write";
    public static final String SCOPE_CC_ADMIN = "cloud_controller.admin";

    // Token Body elements:
    public static final String SCOPE = "scope";
    public static final String EXPIRES_AT_KEY = "exp";
    public static final String ISSUED_AT_KEY = "iat";
    public static final String USER_NAME = "user_name";
    public static final String USER_ID = "user_id";
    public static final String CLIENT_ID = "client_id";

    public OAuth2AccessTokenWithAdditionalInfo createToken(String tokenString) {
        Map<String, Object> tokenInfo = parseToken(tokenString);
        return createToken(tokenString, tokenInfo);
    }

    @SuppressWarnings("unchecked")
    public OAuth2AccessTokenWithAdditionalInfo createToken(String tokenString, Map<String, Object> tokenInfo) {
        List<String> scope = (List<String>) tokenInfo.get(SCOPE);
        Number expiresAt = (Number) tokenInfo.get(EXPIRES_AT_KEY);
        Number instantiatedAt = (Number) tokenInfo.get(ISSUED_AT_KEY);
        if (scope == null || expiresAt == null || instantiatedAt == null) {
            throw new IllegalStateException(MessageFormat.format("One or more of the following elements are missing from the token: \"{0}\"",
                                                                 List.of(SCOPE, EXPIRES_AT_KEY, ISSUED_AT_KEY)));
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
        String[] headerBodySignature = tokenString.split("\\.");
        if (headerBodySignature.length != JWT_TOKEN_PARTS_COUNT) {
            return Collections.emptyMap();
        }
        String body = decode(headerBodySignature[1]);
        return JsonUtil.convertJsonToMap(body);
    }

    private String decode(String string) {
        Decoder decoder = Base64.getUrlDecoder();
        return new String(decoder.decode(string), StandardCharsets.UTF_8);
    }

    public OAuth2AccessTokenWithAdditionalInfo createToken(Oauth2AccessTokenResponse oauth2AccessTokenResponse) {
        return createToken(oauth2AccessTokenResponse.getAccessToken());
    }

}
