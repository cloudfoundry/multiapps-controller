package org.cloudfoundry.multiapps.controller.core.cf;

import java.util.ArrayList;
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
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

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
        CloudControllerRestClient controllerClient = clientFactory.createClient(configuration.getControllerUrl(), credentials, null,
                                                                                oAuthClient);
        addTaggingInterceptor(controllerClient.getRestTemplate());
        return new ResilientCloudControllerClient(controllerClient);
    }

    @Override
    protected CloudControllerClient createClient(CloudCredentials credentials, String org, String space) {
        OAuthClient oAuthClient = oAuthClientFactory.createOAuthClient();
        CloudControllerRestClient controllerClient = clientFactory.createClient(configuration.getControllerUrl(), credentials, org, space,
                                                                                oAuthClient);
        addTaggingInterceptor(controllerClient.getRestTemplate(), org, space);
        return new ResilientCloudControllerClient(controllerClient);
    }

    @Override
    protected CloudControllerClient createClient(CloudCredentials credentials, String spaceId) {
        CloudSpace target = computeTarget(credentials, spaceId);
        OAuthClient oAuthClient = oAuthClientFactory.createOAuthClient();
        CloudControllerRestClient controllerClient = clientFactory.createClient(configuration.getControllerUrl(), credentials, target,
                                                                                oAuthClient);
        addTaggingInterceptor(controllerClient.getRestTemplate(), target.getOrganization()
                                                                        .getName(),
                              target.getName());
        return new ResilientCloudControllerClient(controllerClient);
    }

    private void addTaggingInterceptor(RestTemplate template) {
        addTaggingInterceptor(template, null, null);
    }

    private void addTaggingInterceptor(RestTemplate template, String org, String space) {
        if (template.getInterceptors()
                    .isEmpty()) {
            template.setInterceptors(new ArrayList<>());
        }
        ClientHttpRequestInterceptor requestInterceptor = new TaggingRequestInterceptor(configuration.getVersion(), org, space);
        template.getInterceptors()
                .add(requestInterceptor);
    }

    protected CloudSpace computeTarget(CloudCredentials credentials, String spaceId) {
        CloudControllerClient clientWithoutTarget = createClient(credentials);
        return clientWithoutTarget.getSpace(UUID.fromString(spaceId));
    }
}
