package com.sap.cloud.lm.sl.cf.client;

import org.cloudfoundry.client.lib.oauth2.OauthClient;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

public class CloudFoundryTokenProvider implements TokenProvider {

    private final OauthClient client;

    public CloudFoundryTokenProvider(OauthClient client) {
        this.client = client;
    }

    @Override
    public OAuth2AccessToken getToken() {
        return client.getToken();
    }

}
