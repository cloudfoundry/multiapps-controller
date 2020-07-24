package org.cloudfoundry.multiapps.controller.client;

import org.cloudfoundry.client.lib.oauth2.OAuthClient;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

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
