package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.net.URL;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.cloudfoundry.multiapps.controller.client.facade.CloudCredentials;
import org.cloudfoundry.multiapps.controller.client.facade.adapters.LogCacheClient;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.oauth2.OAuthClient;
import org.cloudfoundry.multiapps.controller.client.facade.util.RestUtil;
import org.cloudfoundry.multiapps.controller.client.facade.util.UriUtil;
import org.immutables.value.Value;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Value.Immutable
public abstract class CloudControllerRestClientFactory {

    private final RestUtil restUtil = new RestUtil();

    private final Map<String, HttpConnectionPair> cachedHttpConnectionPair = new ConcurrentHashMap<>();

    public abstract Optional<Duration> getSslHandshakeTimeout();

    public abstract Optional<Duration> getConnectTimeout();

    public abstract Optional<Integer> getConnectionPoolSize();

    public abstract Optional<Integer> getThreadPoolSize();

    public abstract Optional<Duration> getResponseTimeout();

    @Value.Default
    public boolean shouldTrustSelfSignedCertificates() {
        return false;
    }

    public CloudControllerRestClient createClient(URL controllerUrl, CloudCredentials credentials, String organizationName,
                                                  String spaceName, OAuthClient oAuthClient, Map<String, String> requestTags) {
        oAuthClient.init(credentials);
        CloudSpace target = createSpaceClient(controllerUrl, oAuthClient, requestTags).getSpace(organizationName, spaceName);
        return createClient(controllerUrl, credentials, target, oAuthClient, requestTags);
    }

    public CloudControllerRestClient createClient(URL controllerUrl, CloudCredentials credentials, CloudSpace target) {
        return createClient(controllerUrl, credentials, target, createOAuthClient(controllerUrl),
                            Collections.emptyMap());
    }

    public CloudControllerRestClient createClient(URL controllerUrl, CloudCredentials credentials, CloudSpace target,
                                                  OAuthClient oAuthClient, Map<String, String> requestTags) {
        oAuthClient.init(credentials);
        HttpConnectionPair httpConnectionPair = getCachedHttpConnectionPair(controllerUrl, requestTags);
        URL v3ApiUrl = httpConnectionPair.v3ApiUrl();
        URL baseUrl = UriUtil.extractBaseUrl(v3ApiUrl);

        HttpClient httpClient = httpConnectionPair.httpClient();
        CloudControllerV3Client client = buildCloudControllerV3Client(httpClient, baseUrl, oAuthClient, requestTags);

        Function<Duration, RestClient> uploadRestClientFactory = uploadTimeout -> {
            ClientConfigurationOptions uploadOptions = new ClientConfigurationOptions(getConnectTimeout(), Optional.of(uploadTimeout),
                                                                                      getSslHandshakeTimeout(), getConnectionPoolSize(),
                                                                                      Optional.empty(),
                                                                                      shouldTrustSelfSignedCertificates());

            return CloudControllerRestClientBuilder.build(baseUrl, oAuthClient, requestTags, uploadOptions);
        };

        return new CloudControllerRestClientV3Impl(target, client, uploadRestClientFactory);
    }

    public CloudSpaceClient createSpaceClient(URL controllerUrl, OAuthClient oAuthClient, Map<String, String> requestTags) {
        HttpConnectionPair httpConnectionPair = getCachedHttpConnectionPair(controllerUrl, requestTags);
        URL baseUrl = UriUtil.extractBaseUrl(httpConnectionPair.v3ApiUrl());

        CloudControllerV3Client client = buildCloudControllerV3Client(httpConnectionPair.httpClient(), baseUrl, oAuthClient, requestTags);

        return new CloudSpaceClient(client);
    }

    private CloudControllerV3Client buildCloudControllerV3Client(HttpClient httpClient, URL baseUrl, OAuthClient oAuthClient,
                                                                 Map<String, String> requestTags) {
        RestClient restClient = CloudControllerRestClientBuilder.buildRestClient(httpClient, baseUrl, oAuthClient, requestTags);
        WebClient webClient = CloudControllerRestClientBuilder.buildWebClient(httpClient, baseUrl, oAuthClient, requestTags);

        return new CloudControllerV3Client(restClient, webClient);
    }

    public LogCacheClient createLogCacheClient(URL controllerUrl, OAuthClient oAuthClient, Map<String, String> requestTags) {
        HttpConnectionPair httpConnectionPair = getCachedHttpConnectionPair(controllerUrl, requestTags);

        RestClient restClient = CloudControllerRestClientBuilder.buildRestClient(httpConnectionPair.httpClient(),
                                                                                 httpConnectionPair.logCacheUrl(),
                                                                                 oAuthClient, requestTags);
        return new LogCacheClient(restClient);
    }

    private HttpConnectionPair getCachedHttpConnectionPair(URL controllerUrl, Map<String, String> requestTags) {
        return cachedHttpConnectionPair.computeIfAbsent(controllerUrl.getHost(), _ -> {
            HttpClient httpClient = CloudControllerRestClientBuilder.buildHttpClient(getClientConfigurationOptions());
            Map<String, Object> rootDocumentLinks = CloudControllerRootDocumentClient.getRootDocumentLinks(controllerUrl, requestTags);

            URL v3ApiUrl = UriUtil.resolveV3ApiUrl(controllerUrl, rootDocumentLinks);
            URL logCacheUrl = UriUtil.resolveLogCacheUrl(controllerUrl, rootDocumentLinks);

            return new HttpConnectionPair(httpClient, v3ApiUrl, logCacheUrl);
        });
    }

    private ClientConfigurationOptions getClientConfigurationOptions() {
        return new ClientConfigurationOptions(getConnectTimeout(), getResponseTimeout(), getSslHandshakeTimeout(),
                                              getConnectionPoolSize(), getThreadPoolSize(),
                                              shouldTrustSelfSignedCertificates());
    }

    private OAuthClient createOAuthClient(URL controllerUrl) {
        return restUtil.createOAuthClientByControllerUrl(controllerUrl, shouldTrustSelfSignedCertificates());
    }

}
