package org.cloudfoundry.multiapps.controller.core.cf;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.ResilientCloudControllerClient;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudCredentials;
import com.sap.cloudfoundry.client.facade.domain.CloudSpace;
import com.sap.cloudfoundry.client.facade.oauth2.OAuthClient;
import com.sap.cloudfoundry.client.facade.rest.CloudControllerRestClient;
import com.sap.cloudfoundry.client.facade.rest.CloudControllerRestClientFactory;
import com.sap.cloudfoundry.client.facade.rest.ImmutableCloudControllerRestClientFactory;

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
