package com.sap.cloud.lm.sl.cf.client.util;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.lib.util.JsonUtil;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Component;

@Component
public class TokenFactory {

    public static final String DUMMY_TOKEN = "DUMMY";
    public static final UUID DUMMY_UUID = new UUID(0, 0);

    // Scopes
    public static final String SCOPE_CC_READ = "cloud_controller.read";
    public static final String SCOPE_CC_WRITE = "cloud_controller.write";
    public static final String SCOPE_CC_ADMIN = "cloud_controller.admin";
    public static final String SCOPE_SCIM_USERIDS = "scim.userids";
    public static final String SCOPE_PASSWORD_WRITE = "password.write";
    public static final String SCOPE_OPENID = "openid";

    public OAuth2AccessToken createToken(String tokenString) {
        Map<String, Object> tokenInfo = parseToken(tokenString);
        return createToken(tokenString, tokenInfo);
    }

    @SuppressWarnings("unchecked")
    public OAuth2AccessToken createToken(String tokenString, Map<String, Object> tokenInfo) {
        List<String> scope = (List<String>) tokenInfo.get("scope");
        Integer exp = (Integer) tokenInfo.get("exp");
        if (scope == null || exp == null) {
            return null;
        }
        DefaultOAuth2AccessToken token = new DefaultOAuth2AccessToken(tokenString);
        token.setExpiration(new Date(exp.longValue() * 1000));
        token.setScope(new HashSet<String>(scope));
        token.setAdditionalInformation(tokenInfo);
        return token;
    }

    public OAuth2AccessToken createDummyToken(String userName, String clientId) {
        List<String> scope = Arrays.asList(SCOPE_CC_READ, SCOPE_CC_WRITE, SCOPE_CC_ADMIN, SCOPE_SCIM_USERIDS, SCOPE_PASSWORD_WRITE,
            SCOPE_OPENID);
        Map<String, Object> tokenInfo = new HashMap<>();
        tokenInfo.put("scope", scope);
        tokenInfo.put("exp", Long.MAX_VALUE / 1000);
        tokenInfo.put("user_name", userName);
        tokenInfo.put("user_id", DUMMY_UUID.toString());
        tokenInfo.put("client_id", clientId);
        DefaultOAuth2AccessToken token = new DefaultOAuth2AccessToken(DUMMY_TOKEN);
        token.setExpiration(new Date(Long.MAX_VALUE));
        token.setScope(new HashSet<String>(scope));
        token.setAdditionalInformation(tokenInfo);
        return token;
    }

    private Map<String, Object> parseToken(String tokenString) {
        String[] tokenParts = tokenString.split("\\.", 3);
        if (tokenParts.length != 3) {
            // The token should have three parts (header, body and signature) separated by a dot. It doesn't, so we consider it as invalid.
            return Collections.emptyMap();
        }
        String body = decode(tokenParts[1]);
        return JsonUtil.convertJsonToMap(body);
    }

    private String decode(String string) {
        Decoder decoder = Base64.getDecoder();
        return new String(decoder.decode(string), StandardCharsets.UTF_8);
    }

}
