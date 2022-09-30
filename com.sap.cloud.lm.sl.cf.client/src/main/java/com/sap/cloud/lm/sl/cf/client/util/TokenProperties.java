package com.sap.cloud.lm.sl.cf.client.util;

import java.util.Map;

import org.springframework.security.oauth2.common.OAuth2AccessToken;

public class TokenProperties {

    private static final String CLIENT_ID_KEY = "client_id";
    private static final String USER_NAME_KEY = "user_name";
    private static final String USER_ID_KEY = "user_id";

    private String clientId;
    private String userName;
    private String userId;

    public TokenProperties(String clientId, String userId, String userName) {
        this.clientId = clientId;
        this.userName = userName;
        this.userId = userId;
    }

    public static TokenProperties fromToken(OAuth2AccessToken token) {
        String clientId = getTokenProperty(token, CLIENT_ID_KEY);
        String userName = getTokenProperty(token, USER_NAME_KEY);
        String userId = getTokenProperty(token, USER_ID_KEY);
        return new TokenProperties(clientId, userId, userName);
    }

    @SuppressWarnings("unchecked")
    private static <T> T getTokenProperty(OAuth2AccessToken token, String key) {
        Map<String, Object> additionalInformation = token.getAdditionalInformation();
        return (T) additionalInformation.get(key);
    }

    public String getClientId() {
        return clientId;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserId() {
        return userId;
    }

}
