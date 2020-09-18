package org.cloudfoundry.multiapps.controller.core.cf;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.oauth2.OAuthClient;
import org.cloudfoundry.client.lib.util.RestUtil;
import org.cloudfoundry.multiapps.controller.client.uaa.UAAClient;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.springframework.web.reactive.function.client.WebClient;

@Named
public class OAuthClientFactory {

    private final RestUtil restUtil = new RestUtil();
    @Inject
    private TokenService tokenService;
    @Inject
    private UAAClient uaaClient;
    @Inject
    private ApplicationConfiguration configuration;

    public OAuthClient createOAuthClient() {
        WebClient webClient = createWebClient();
        return new OAuthClientExtended(uaaClient.getUaaUrl(), webClient, tokenService);
    }

    private WebClient createWebClient() {
        return restUtil.createWebClient(configuration.shouldSkipSslValidation());
    }

}
