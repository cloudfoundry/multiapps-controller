package org.cloudfoundry.multiapps.controller.client;

import org.springframework.security.oauth2.common.OAuth2AccessToken;

import com.sap.cloudfoundry.client.facade.oauth2.OAuthClient;

public class CloudFoundryTokenProvider implements TokenProvider {

    private final OAuthClient client;

    public CloudFoundryTokenProvider(OAuthClient client) {
        this.client = client;
    }

    @Override
    public OAuth2AccessToken getToken() {
        return client.getToken();
    }

}
