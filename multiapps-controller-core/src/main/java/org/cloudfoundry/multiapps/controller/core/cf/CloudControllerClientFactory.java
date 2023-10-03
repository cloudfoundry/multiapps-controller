package org.cloudfoundry.multiapps.controller.core.cf;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.multiapps.controller.client.ResilientCloudControllerClient;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudCredentials;
import com.sap.cloudfoundry.client.facade.adapters.LogCacheClient;
import com.sap.cloudfoundry.client.facade.domain.CloudSpace;
import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import com.sap.cloudfoundry.client.facade.oauth2.OAuthClient;
import com.sap.cloudfoundry.client.facade.rest.CloudControllerRestClient;
import com.sap.cloudfoundry.client.facade.rest.CloudControllerRestClientFactory;
import com.sap.cloudfoundry.client.facade.rest.CloudSpaceClient;
import com.sap.cloudfoundry.client.facade.rest.ImmutableCloudControllerRestClientFactory;

@Named
public class CloudControllerClientFactory {

    private final ApplicationConfiguration configuration;
    private final CloudControllerRestClientFactory clientFactory;
    private final OAuthClientFactory oAuthClientFactory;
    private final CloudControllerHeaderConfiguration headerConfiguration;

    @Inject
    public CloudControllerClientFactory(ApplicationConfiguration configuration, OAuthClientFactory oAuthClientFactory) {
        this.clientFactory = createClientFactory(configuration);
        this.configuration = configuration;
        this.oAuthClientFactory = oAuthClientFactory;
        this.headerConfiguration = new CloudControllerHeaderConfiguration(configuration.getVersion());
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
        CloudControllerRestClient controllerClient = clientFactory.createClient(configuration.getControllerUrl(), createCredentials(token),
                                                                                null, oAuthClient, Collections.emptyMap());
        return new ResilientCloudControllerClient(controllerClient);
    }

    public CloudControllerClient createClient(OAuth2AccessTokenWithAdditionalInfo token, String org, String space, String correlationId) {
        OAuthClient oAuthClient = oAuthClientFactory.createOAuthClient();
        Map<String, String> requestTags = buildRequestTags(correlationId);
        CloudControllerRestClient controllerClient = clientFactory.createClient(configuration.getControllerUrl(), createCredentials(token),
                                                                                org, space, oAuthClient, requestTags);
        return new ResilientCloudControllerClient(controllerClient);
    }

    public CloudControllerClient createClient(OAuth2AccessTokenWithAdditionalInfo token, String spaceId, String correlationId) {
        Map<String, String> requestTags = buildRequestTags(correlationId);
        OAuthClient oAuthClient = oAuthClientFactory.createOAuthClient();
        CloudCredentials credentials = createCredentials(token);
        oAuthClient.init(credentials);

        var spaceClient = clientFactory.getCloudFoundryClientFactory()
                                       .createSpaceClient(configuration.getControllerUrl(), oAuthClient, requestTags);
        CloudSpace target = spaceClient.getSpace(UUID.fromString(spaceId));

        CloudControllerRestClient controllerClient = clientFactory.createClient(configuration.getControllerUrl(), credentials, target,
                                                                                oAuthClient, requestTags);
        return new ResilientCloudControllerClient(controllerClient);
    }

    public CloudSpaceClient createSpaceClient(OAuth2AccessTokenWithAdditionalInfo token) {
        OAuthClient oAuthClient = oAuthClientFactory.createOAuthClient();
        CloudCredentials credentials = createCredentials(token);
        oAuthClient.init(credentials);
        var requestTags = buildRequestTags(StringUtils.EMPTY);
        return clientFactory.getCloudFoundryClientFactory()
                            .createSpaceClient(configuration.getControllerUrl(), oAuthClient, requestTags);
    }

    public LogCacheClient createLogCacheClient(OAuth2AccessTokenWithAdditionalInfo token, String correlationId) {
        OAuthClient oAuthClient = oAuthClientFactory.createOAuthClient();
        CloudCredentials credentials = createCredentials(token);
        oAuthClient.init(credentials);
        var requestTags = buildRequestTags(correlationId);
        return clientFactory.getCloudFoundryClientFactory()
                            .createLogCacheClient(configuration.getControllerUrl(), oAuthClient, requestTags);
    }

    private CloudCredentials createCredentials(OAuth2AccessTokenWithAdditionalInfo token) {
        return new CloudCredentials(token, true);
    }

    private Map<String, String> buildRequestTags(String correlationId) {
        return headerConfiguration.generateHeaders(correlationId);
    }
}
