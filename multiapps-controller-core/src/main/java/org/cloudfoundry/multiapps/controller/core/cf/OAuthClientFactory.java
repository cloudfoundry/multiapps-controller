package org.cloudfoundry.multiapps.controller.core.cf;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.uaa.UAAClient;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;

import com.sap.cloudfoundry.client.facade.oauth2.OAuthClient;
import com.sap.cloudfoundry.client.facade.util.RestUtil;

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
