package org.cloudfoundry.multiapps.controller.core.cf;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.uaa.UAAClient;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;

import com.sap.cloudfoundry.client.facade.oauth2.OAuthClient;

@Named
public class OAuthClientFactory {

    @Inject
    private TokenService tokenService;
    @Inject
    private UAAClient uaaClient;

    public OAuthClient createOAuthClient() {
        return new OAuthClientExtended(uaaClient.getUaaUrl(), tokenService);
    }

}
