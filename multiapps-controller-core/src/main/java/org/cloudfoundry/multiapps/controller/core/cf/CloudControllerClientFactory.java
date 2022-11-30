package org.cloudfoundry.multiapps.controller.core.cf;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.ResilientCloudControllerClient;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudCredentials;
import com.sap.cloudfoundry.client.facade.domain.CloudSpace;
import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import com.sap.cloudfoundry.client.facade.oauth2.OAuthClient;
import com.sap.cloudfoundry.client.facade.rest.CloudControllerRestClient;
import com.sap.cloudfoundry.client.facade.rest.CloudControllerRestClientFactory;
import com.sap.cloudfoundry.client.facade.rest.ImmutableCloudControllerRestClientFactory;

@Named
public class CloudControllerClientFactory {

    private final ApplicationConfiguration configuration;
    private final CloudControllerRestClientFactory clientFactory;
    private final OAuthClientFactory oAuthClientFactory;
    private CloudControllerHeaderConfiguration headerConfiguration;

    @Inject
    public CloudControllerClientFactory(ApplicationConfiguration configuration, OAuthClientFactory oAuthClientFactory,
                                        CloudControllerHeaderConfiguration headerConfiguration) {
        this.clientFactory = createClientFactory(configuration);
        this.configuration = configuration;
        this.oAuthClientFactory = oAuthClientFactory;
        this.headerConfiguration = headerConfiguration;
    }

    private ImmutableCloudControllerRestClientFactory createClientFactory(ApplicationConfiguration configuration) {
        return ImmutableCloudControllerRestClientFactory.builder()
                                                        .sslHandshakeTimeout(configuration.getControllerClientSslHandshakeTimeout())
                                                        .connectTimeout(configuration.getControllerClientConnectTimeout())
                                                        .connectionPoolSize(configuration.getControllerClientConnectionPoolSize())
                                                        .threadPoolSize(configuration.getControllerClientThreadPoolSize())
                                                        .responseTimeout(configuration.getControllerClientResponseTimeout())
                                                        .shouldTrustSelfSignedCertificates(configuration.shouldSkipSslValidation())
                                                        .build();
    }

    public CloudControllerClient createClient(OAuth2AccessTokenWithAdditionalInfo token) {
        OAuthClient oAuthClient = oAuthClientFactory.createOAuthClient();
        List<ExchangeFilterFunction> exchangeFilters = getExchangeFiltersList(null, null, null);
        CloudControllerRestClient controllerClient = clientFactory.createClient(configuration.getControllerUrl(), createCredentials(token),
                                                                                null, oAuthClient, exchangeFilters, Collections.emptyMap());
        return new ResilientCloudControllerClient(controllerClient);
    }

    public CloudControllerClient createClient(OAuth2AccessTokenWithAdditionalInfo token, String org, String space, String correlationId) {
        OAuthClient oAuthClient = oAuthClientFactory.createOAuthClient();
        List<ExchangeFilterFunction> exchangeFilters = getExchangeFiltersList(org, space, correlationId);
        Map<String, String> requestTags = buildRequestTags(correlationId);
        CloudControllerRestClient controllerClient = clientFactory.createClient(configuration.getControllerUrl(), createCredentials(token),
                                                                                org, space, oAuthClient, exchangeFilters, requestTags);
        return new ResilientCloudControllerClient(controllerClient);
    }

    public CloudControllerClient createClient(OAuth2AccessTokenWithAdditionalInfo token, String spaceId, String correlationId) {
        CloudSpace target = computeTarget(token, spaceId);
        OAuthClient oAuthClient = oAuthClientFactory.createOAuthClient();
        List<ExchangeFilterFunction> exchangeFilters = getExchangeFiltersList(target.getOrganization()
                                                                                    .getName(),
                                                                              target.getName(), correlationId);
        Map<String, String> requestTags = buildRequestTags(correlationId);
        CloudControllerRestClient controllerClient = clientFactory.createClient(configuration.getControllerUrl(), createCredentials(token),
                                                                                target, oAuthClient, exchangeFilters, requestTags);
        return new ResilientCloudControllerClient(controllerClient);
    }

    private CloudCredentials createCredentials(OAuth2AccessTokenWithAdditionalInfo token) {
        return new CloudCredentials(token, true);
    }

    private Map<String, String> buildRequestTags(String correlationId) {
        return headerConfiguration.generateHeaders(correlationId);

    }

    private List<ExchangeFilterFunction> getExchangeFiltersList(String org, String space, String correlationId) {
        return List.of(new TaggingRequestFilterFunction(configuration.getVersion(), org, space, correlationId));
    }

    protected CloudSpace computeTarget(OAuth2AccessTokenWithAdditionalInfo token, String spaceId) {
        CloudControllerClient clientWithoutTarget = createClient(token);
        return clientWithoutTarget.getSpace(UUID.fromString(spaceId));
    }
}
