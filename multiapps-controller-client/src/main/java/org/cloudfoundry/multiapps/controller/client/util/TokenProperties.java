package org.cloudfoundry.multiapps.controller.client.util;

import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;

import java.util.Map;

public class TokenProperties {

    public static final String CLIENT_ID_KEY = "client_id";
    public static final String USER_NAME_KEY = "user_name";
    public static final String USER_ID_KEY = "user_id";

    private final String clientId;
    private final String userName;
    private final String userId;

    public TokenProperties(String clientId, String userId, String userName) {
        this.clientId = clientId;
        this.userName = userName;
        this.userId = userId;
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

    public static TokenProperties fromToken(OAuth2AccessTokenWithAdditionalInfo token) {
        String clientId = getTokenProperty(token, CLIENT_ID_KEY);
        String userName = getTokenProperty(token, USER_NAME_KEY);
        String userId = getTokenProperty(token, USER_ID_KEY);
        return new TokenProperties(clientId, userId, userName);
    }

    @SuppressWarnings("unchecked")
    private static <T> T getTokenProperty(OAuth2AccessTokenWithAdditionalInfo token, String key) {
        Map<String, Object> additionalInformation = token.getAdditionalInfo();
        return (T) additionalInformation.get(key);
    }

}
