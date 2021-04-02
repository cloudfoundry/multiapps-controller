package org.cloudfoundry.multiapps.controller.core.cf;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
                                                        .responseTimeout(configuration.getControllerClientResponseTimeout())
                                                        .shouldTrustSelfSignedCertificates(configuration.shouldSkipSslValidation())
                                                        .build();
    }

    @Override
    protected CloudControllerClient createClient(CloudCredentials credentials) {
        OAuthClient oAuthClient = oAuthClientFactory.createOAuthClient();
        List<ExchangeFilterFunction> exchangeFilters = getExchangeFiltersList(null, null,null);
        CloudControllerRestClient controllerClient = clientFactory.createClient(configuration.getControllerUrl(), credentials, null,
                                                                                oAuthClient, exchangeFilters, Collections.emptyMap());
        return new ResilientCloudControllerClient(controllerClient);
    }

    @Override
    protected CloudControllerClient createClient(CloudCredentials credentials, String org, String space, String correlationId) {
        OAuthClient oAuthClient = oAuthClientFactory.createOAuthClient();
        List<ExchangeFilterFunction> exchangeFilters = getExchangeFiltersList(org, space, correlationId);
        Map<String, String> requestTags = buildCorrelationIdTag(correlationId);
        CloudControllerRestClient controllerClient = clientFactory.createClient(configuration.getControllerUrl(), credentials, org, space,
                                                                                oAuthClient, exchangeFilters, requestTags);
        return new ResilientCloudControllerClient(controllerClient);
    }

    @Override
    protected CloudControllerClient createClient(CloudCredentials credentials, String spaceId, String correlationId) {
        CloudSpace target = computeTarget(credentials, spaceId);
        OAuthClient oAuthClient = oAuthClientFactory.createOAuthClient();
        List<ExchangeFilterFunction> exchangeFilters = getExchangeFiltersList(target.getOrganization()
                                                                                    .getName(),
                                                                              target.getName(),
                                                                              correlationId);
        Map<String, String> requestTags = buildCorrelationIdTag(correlationId);
        CloudControllerRestClient controllerClient = clientFactory.createClient(configuration.getControllerUrl(), credentials, target,
                                                                                oAuthClient, exchangeFilters, requestTags);
        return new ResilientCloudControllerClient(controllerClient);
    }

    private Map<String, String> buildCorrelationIdTag(String correlationId) {
        return Optional.ofNullable(correlationId)
                       .map(correlationIdValue -> Map.of(TaggingRequestFilterFunction.TAG_HEADER_CORRELATION_ID, correlationIdValue))
                       .orElse(Collections.emptyMap());
    }

    private List<ExchangeFilterFunction> getExchangeFiltersList(String org, String space, String correlationId) {
        return List.of(new TaggingRequestFilterFunction(configuration.getVersion(), org, space, correlationId));
    }

    protected CloudSpace computeTarget(CloudCredentials credentials, String spaceId) {
        CloudControllerClient clientWithoutTarget = createClient(credentials);
        return clientWithoutTarget.getSpace(UUID.fromString(spaceId));
    }
}
