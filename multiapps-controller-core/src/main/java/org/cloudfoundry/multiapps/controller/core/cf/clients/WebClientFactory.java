package org.cloudfoundry.multiapps.controller.core.cf.clients;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.client.facade.CloudCredentials;
import org.cloudfoundry.multiapps.controller.client.facade.util.RestUtil;
import org.cloudfoundry.multiapps.controller.core.cf.OAuthClientFactory;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.springframework.web.reactive.function.client.WebClient;

@Named
public class WebClientFactory {

    @Inject
    private ApplicationConfiguration configuration;
    @Inject
    private OAuthClientFactory oAuthClientFactory;

    public WebClient getWebClient(CloudCredentials credentials) {
        WebClient.Builder webClientBuilder = new RestUtil().createWebClient(false)
                                                           .mutate()
                                                           .baseUrl(configuration.getControllerUrl()
                                                                                 .toString());
        webClientBuilder.defaultHeaders(httpHeaders -> httpHeaders.setBearerAuth(computeAuthorizationToken(credentials)));
        return webClientBuilder.build();
    }

    private String computeAuthorizationToken(CloudCredentials credentials) {
        var oAuthClient = oAuthClientFactory.createOAuthClient();
        oAuthClient.init(credentials);
        return oAuthClient.getToken()
                          .getOAuth2AccessToken()
                          .getTokenValue();
    }

}
