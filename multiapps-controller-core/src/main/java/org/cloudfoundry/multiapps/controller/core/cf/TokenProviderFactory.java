package org.cloudfoundry.multiapps.controller.core.cf;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.CloudFoundryTokenProvider;
import org.cloudfoundry.multiapps.controller.client.TokenProvider;
import org.cloudfoundry.multiapps.controller.core.util.SecurityUtil;

import com.sap.cloudfoundry.client.facade.CloudCredentials;
import com.sap.cloudfoundry.client.facade.oauth2.OAuthClient;

@Named
public class TokenProviderFactory {

    private final OAuthClientFactory oAuthClientFactory;

    @Inject
    public TokenProviderFactory(OAuthClientFactory oAuthClientFactory) {
        this.oAuthClientFactory = oAuthClientFactory;
    }

    public TokenProvider createTokenProvider(String username, String password) {
        CloudCredentials credentials = createCredentials(username, password);
        OAuthClient oAuthClient = oAuthClientFactory.createOAuthClient();
        oAuthClient.init(credentials);
        return new CloudFoundryTokenProvider(oAuthClient);
    }

    private static CloudCredentials createCredentials(String userName, String password) {
        return new CloudCredentials(userName, password, SecurityUtil.CLIENT_ID, SecurityUtil.CLIENT_SECRET);
    }
}
