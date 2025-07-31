package org.cloudfoundry.multiapps.controller.client;

import org.cloudfoundry.multiapps.controller.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import org.cloudfoundry.multiapps.controller.client.facade.oauth2.OAuthClient;

public class CloudFoundryTokenProvider implements TokenProvider {

    private final OAuthClient client;

    public CloudFoundryTokenProvider(OAuthClient client) {
        this.client = client;
    }

    @Override
    public OAuth2AccessTokenWithAdditionalInfo getToken() {
        return client.getToken();
    }

}
