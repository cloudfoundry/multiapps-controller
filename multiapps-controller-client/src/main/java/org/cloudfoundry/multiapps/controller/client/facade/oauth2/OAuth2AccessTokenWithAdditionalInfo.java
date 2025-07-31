package org.cloudfoundry.multiapps.controller.client.facade.oauth2;

import java.util.Map;

import org.springframework.security.oauth2.core.OAuth2AccessToken;

public class OAuth2AccessTokenWithAdditionalInfo {

    private OAuth2AccessToken oAuth2AccessToken;
    private Map<String, Object> additionalInfo;

    public OAuth2AccessTokenWithAdditionalInfo(OAuth2AccessToken oAuth2AccessToken) {
        this.oAuth2AccessToken = oAuth2AccessToken;
    }

    public OAuth2AccessTokenWithAdditionalInfo(OAuth2AccessToken oAuth2AccessToken, Map<String, Object> additionalInfo) {
        this.oAuth2AccessToken = oAuth2AccessToken;
        this.additionalInfo = additionalInfo;
    }

    public OAuth2AccessToken getOAuth2AccessToken() {
        return oAuth2AccessToken;
    }

    public Map<String, Object> getAdditionalInfo() {
        return additionalInfo;
    }

    public String getAuthorizationHeaderValue() {
        return getOAuth2AccessToken().getTokenType()
                                     .getValue()
            + " " + getOAuth2AccessToken().getTokenValue();
    }

}
