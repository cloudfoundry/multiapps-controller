package org.cloudfoundry.multiapps.controller.core.cf.clients;

import com.sap.cloudfoundry.client.facade.CloudCredentials;
import org.cloudfoundry.multiapps.controller.client.uaa.UAAClientFactory;
import org.cloudfoundry.multiapps.controller.core.cf.OAuthClientFactory;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.springframework.web.reactive.function.client.WebClient;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class WebClientFactory {

    @Inject
    private ApplicationConfiguration configuration;
    @Inject
    private OAuthClientFactory oAuthClientFactory;

    public WebClient getWebClient(CloudCredentials credentials) {
        WebClient webClient = new UAAClientFactory().buildWebClientWith(configuration.shouldSkipSslValidation());
        webClient.mutate()
                .baseUrl(configuration.getControllerUrl()
                        .toString()).defaultHeaders(httpHeaders -> httpHeaders.setBearerAuth(computeAuthorizationToken(credentials)));
        return webClient;
    }

    private String computeAuthorizationToken(CloudCredentials credentials) {
        var oAuthClient = oAuthClientFactory.createOAuthClient();
        oAuthClient.init(credentials);
        return oAuthClient.getToken()
                .getOAuth2AccessToken()
                .getTokenValue();
    }

}
