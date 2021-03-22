package org.cloudfoundry.multiapps.controller.core.cf.clients;

import javax.inject.Named;

import org.springframework.web.reactive.function.client.WebClient;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.util.RestUtil;

@Named
public class WebClientFactory {

    public WebClient getWebClient(CloudControllerClient client) {
        WebClient.Builder webClientBuilder = new RestUtil().createWebClient(false)
                                                           .mutate()
                                                           .baseUrl(client.getCloudControllerUrl()
                                                                          .toString());
        webClientBuilder.defaultHeaders(httpHeaders -> httpHeaders.setBearerAuth(computeAuthorizationToken(client)));
        return webClientBuilder.build();
    }

    private String computeAuthorizationToken(CloudControllerClient client) {
        return client.login()
                     .getOAuth2AccessToken()
                     .getTokenValue();
    }

}
