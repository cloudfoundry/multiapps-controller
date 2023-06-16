package org.cloudfoundry.multiapps.controller.core.cf.clients;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.cf.OAuthClientFactory;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.springframework.web.reactive.function.client.WebClient;

import com.sap.cloudfoundry.client.facade.CloudCredentials;
import com.sap.cloudfoundry.client.facade.util.RestUtil;

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
