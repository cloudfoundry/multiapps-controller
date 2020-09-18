package org.cloudfoundry.multiapps.controller.core.cf;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.oauth2.OAuthClient;
import org.cloudfoundry.client.lib.rest.CloudControllerRestClient;
import org.cloudfoundry.client.lib.rest.CloudControllerRestClientFactory;
import org.cloudfoundry.client.lib.rest.ImmutableCloudControllerRestClientFactory;
import org.cloudfoundry.multiapps.controller.client.ResilientCloudControllerClient;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

@Named
public class CloudFoundryClientFactory extends ClientFactory {

    private final ApplicationConfiguration configuration;
    private final CloudControllerRestClientFactory clientFactory;
    private final OAuthClientFactory oAuthClientFactory;

    @Inject
    public CloudFoundryClientFactory(ApplicationConfiguration configuration, OAuthClientFactory oAuthClientFactory) {
        this.clientFactory = createClientFactory(configuration);
        this.configuration = configuration;
        this.oAuthClientFactory = oAuthClientFactory;
    }

    private ImmutableCloudControllerRestClientFactory createClientFactory(ApplicationConfiguration configuration) {
        return ImmutableCloudControllerRestClientFactory.builder()
                                                        .sslHandshakeTimeout(configuration.getControllerClientSslHandshakeTimeout())
                                                        .connectTimeout(configuration.getControllerClientConnectTimeout())
                                                        .connectionPoolSize(configuration.getControllerClientConnectionPoolSize())
                                                        .threadPoolSize(configuration.getControllerClientThreadPoolSize())
                                                        .shouldTrustSelfSignedCertificates(configuration.shouldSkipSslValidation())
                                                        .build();
    }

    @Override
    protected CloudControllerClient createClient(CloudCredentials credentials) {
        OAuthClient oAuthClient = oAuthClientFactory.createOAuthClient();
        List<ExchangeFilterFunction> exchangeFilters = getExchangeFiltersList(null, null);
        CloudControllerRestClient controllerClient = clientFactory.createClient(configuration.getControllerUrl(), credentials, null,
                                                                                oAuthClient, exchangeFilters);
        return new ResilientCloudControllerClient(controllerClient);
    }

    @Override
    protected CloudControllerClient createClient(CloudCredentials credentials, String org, String space) {
        OAuthClient oAuthClient = oAuthClientFactory.createOAuthClient();
        List<ExchangeFilterFunction> exchangeFilters = getExchangeFiltersList(org, space);
        CloudControllerRestClient controllerClient = clientFactory.createClient(configuration.getControllerUrl(), credentials, org, space,
                                                                                oAuthClient, exchangeFilters);
        return new ResilientCloudControllerClient(controllerClient);
    }

    @Override
    protected CloudControllerClient createClient(CloudCredentials credentials, String spaceId) {
        CloudSpace target = computeTarget(credentials, spaceId);
        OAuthClient oAuthClient = oAuthClientFactory.createOAuthClient();
        List<ExchangeFilterFunction> exchangeFilters = getExchangeFiltersList(target.getOrganization()
                                                                                    .getName(),
                                                                              target.getName());
        CloudControllerRestClient controllerClient = clientFactory.createClient(configuration.getControllerUrl(), credentials, target,
                                                                                oAuthClient, exchangeFilters);
        return new ResilientCloudControllerClient(controllerClient);
    }

    private List<ExchangeFilterFunction> getExchangeFiltersList(String org, String space) {
        return List.of(new TaggingRequestFilterFunction(configuration.getVersion(), org, space));
    }

    protected CloudSpace computeTarget(CloudCredentials credentials, String spaceId) {
        CloudControllerClient clientWithoutTarget = createClient(credentials);
        return clientWithoutTarget.getSpace(UUID.fromString(spaceId));
    }
}
