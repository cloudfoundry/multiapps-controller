package org.cloudfoundry.multiapps.controller.core.cf;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.client.facade.oauth2.OAuthClient;
import org.cloudfoundry.multiapps.controller.client.facade.util.RestUtil;
import org.cloudfoundry.multiapps.controller.client.uaa.UAAClient;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;

@Named
public class OAuthClientFactory {

    private final RestUtil restUtil = new RestUtil();

    @Inject
    private TokenService tokenService;
    @Inject
    private UAAClient uaaClient;

    public OAuthClient createOAuthClient() {
        return new OAuthClientExtended(uaaClient.getUaaUrl(), tokenService, restUtil.createWebClient(true));
    }

}
