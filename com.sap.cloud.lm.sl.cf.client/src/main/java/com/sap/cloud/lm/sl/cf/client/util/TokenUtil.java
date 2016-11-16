package com.sap.cloud.lm.sl.cf.client.util;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.lib.util.JsonUtil;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

public class TokenUtil {

    public static final String DUMMY_TOKEN = "DUMMY";
    public static final UUID DUMMY_UUID = new UUID(0, 0);

    // Scopes
    public static final String SCOPE_CC_READ = "cloud_controller.read";
    public static final String SCOPE_CC_WRITE = "cloud_controller.write";
    public static final String SCOPE_CC_ADMIN = "cloud_controller.admin";
    public static final String SCOPE_SCIM_USERIDS = "scim.userids";
    public static final String SCOPE_PASSWORD_WRITE = "password.write";
    public static final String SCOPE_OPENID = "openid";

    @SuppressWarnings("unchecked")
    public static OAuth2AccessToken createToken(String tokenString) {
        Map<String, Object> tokenInfo = getTokenInfo(tokenString);
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

    public static OAuth2AccessToken createDummyToken(String userName, String clientId) {
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

    public static String getTokenClientId(OAuth2AccessToken token) {
        return (String) getTokenProperty(token, "client_id");
    }

    public static String getTokenUserName(OAuth2AccessToken token) {
        return (String) getTokenProperty(token, "user_name");
    }

    public static String getTokenUserId(OAuth2AccessToken token) {
        return (String) getTokenProperty(token, "user_id");
    }

    public static String asString(OAuth2AccessToken token) {
        if (token != null) {
            return MessageFormat.format("'{' value: {0}, refreshToken: {1}, expiresIn: {2} '}'", token.getValue(), token.getRefreshToken(),
                token.getExpiresIn());
        } else {
            return "null";
        }
    }

    private static Object getTokenProperty(OAuth2AccessToken token, String key) {
        Object value = token.getAdditionalInformation().get(key);
        if (value == null) {
            value = getTokenInfo(token.getValue()).get(key);
        }
        return value;
    }

    @SuppressWarnings("restriction")
    private static Map<String, Object> getTokenInfo(String tokenString) {
        String userJson = "{}";
        try {
            int x = tokenString.indexOf('.');
            int y = tokenString.indexOf('.', x + 1);
            String encodedString = tokenString.substring(x + 1, y);
            byte[] decodedBytes = new sun.misc.BASE64Decoder().decodeBuffer(encodedString);
            userJson = new String(decodedBytes, 0, decodedBytes.length, "UTF-8");
        } catch (IndexOutOfBoundsException e) {
            // Do nothing
        } catch (IOException e) {
            // Do nothing
        }
        return (JsonUtil.convertJsonToMap(userJson));
    }

}
