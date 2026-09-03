package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.cloudfoundry.multiapps.controller.Constants;
import org.cloudfoundry.multiapps.controller.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.CloudCredentials;
import org.cloudfoundry.multiapps.controller.client.facade.CloudException;
import org.cloudfoundry.multiapps.controller.client.facade.adapters.LogCacheClient;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.oauth2.OAuthClient;
import org.cloudfoundry.multiapps.controller.client.facade.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.facade.util.RestUtil;
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
        URL baseUrl = extractBaseUrl(v3ApiUrl);

        HttpClient httpClient = httpConnectionPair.httpClient();
        RestClient restClient = CloudControllerRestClientBuilder.buildRestClient(httpClient, baseUrl, oAuthClient, requestTags);
        WebClient webClient = CloudControllerRestClientBuilder.buildWebClient(httpClient, baseUrl, oAuthClient, requestTags);

        CloudControllerV3Client client = new CloudControllerV3Client(restClient, webClient);

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
        URL baseUrl = extractBaseUrl(httpConnectionPair.v3ApiUrl());

        RestClient restClient = CloudControllerRestClientBuilder.buildRestClient(httpConnectionPair.httpClient(), baseUrl, oAuthClient,
                                                                                 requestTags);
        WebClient webClient = CloudControllerRestClientBuilder.buildWebClient(httpConnectionPair.httpClient(), baseUrl, oAuthClient,
                                                                              requestTags);

        return new CloudSpaceClient(new CloudControllerV3Client(restClient, webClient));
    }

    public LogCacheClient createLogCacheClient(URL controllerUrl, OAuthClient oAuthClient, Map<String, String> requestTags) {
        HttpConnectionPair httpConnectionPair = getCachedHttpConnectionPair(controllerUrl, requestTags);
        RestClient restClient = CloudControllerRestClientBuilder.buildRestClient(httpConnectionPair.httpClient(),
                                                                                 httpConnectionPair.logCacheUrl(),
                                                                                 oAuthClient, requestTags);
        return new LogCacheClient(restClient);
    }

    private HttpConnectionPair getCachedHttpConnectionPair(URL controllerUrl, Map<String, String> requestTags) {
        return cachedHttpConnectionPair.computeIfAbsent(controllerUrl.getHost(), host -> {
            HttpClient httpClient = CloudControllerRestClientBuilder.buildHttpClient(getClientConfigurationOptions());
            URL v3ApiUrl = resolveV3ApiUrl(controllerUrl, requestTags);
            URL logCacheUrl = resolveLogCacheUrl(controllerUrl, requestTags);
            return new HttpConnectionPair(httpClient, v3ApiUrl, logCacheUrl);
        });
    }

    private ClientConfigurationOptions getClientConfigurationOptions() {
        return new ClientConfigurationOptions(getConnectTimeout(), getResponseTimeout(), getSslHandshakeTimeout(),
                                              getConnectionPoolSize(), getThreadPoolSize(),
                                              shouldTrustSelfSignedCertificates());
    }

    private URL extractBaseUrl(URL url) {
        try {
            int port = url.getPort();
            String origin = url.getProtocol() + Constants.PROTOCOL_SEPARATOR + url.getHost() + (port == Constants.UNDEFINED_PORT
                ? Constants.EMPTY_STRING
                : Constants.COLON + port);
            return URI.create(origin)
                      .toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(MessageFormat.format(Messages.COULD_NOT_DETERMINE_API_ORIGIN_FROM_0, url), e);
        }
    }

    private URL resolveV3ApiUrl(URL controllerUrl, Map<String, String> requestTags) {
        try {
            Map<String, Object> rootDocumentLinks = getCloudControllerRootDocumentLinks(controllerUrl, requestTags);
            Map<String, Object> cloudControllerUrlV3 = (Map<String, Object>) rootDocumentLinks.get(
                Constants.CLOUD_CONTROLLER_CF_ROOT_DOCUMENT_NAME);
            return URI.create((String) cloudControllerUrlV3.get(Constants.HREF))
                      .toURL();
        } catch (Exception e) {
            return fallbackUrl(controllerUrl, Constants.CF_API_V3);
        }
    }

    private URL resolveLogCacheUrl(URL controllerUrl, Map<String, String> requestTags) {
        try {
            Map<String, Object> rootDocumentLinks = getCloudControllerRootDocumentLinks(controllerUrl, requestTags);
            Map<String, Object> logCacheUrl = (Map<String, Object>) rootDocumentLinks.get(Constants.LOG_CACHE_CF_ROOT_DOCUMENT_NAME);
            return URI.create((String) logCacheUrl.get(Constants.HREF))
                      .toURL();
        } catch (Exception e) {
            String host = controllerUrl.getHost();
            String logCacheHost = host.startsWith(Constants.API_HOST_PREFIX) ? Constants.LOG_CACHE_PREFIX + host.substring(
                Constants.API_HOST_PREFIX.length()) : Constants.LOG_CACHE_PREFIX + host;
            try {
                return URI.create(controllerUrl.getProtocol() + Constants.PROTOCOL_SEPARATOR + logCacheHost)
                          .toURL();
            } catch (MalformedURLException me) {
                throw new CloudException(MessageFormat.format(Messages.COULD_NOT_RESOLVE_LOG_CACHE_URL_FROM_0, controllerUrl), me);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getCloudControllerRootDocumentLinks(URL controllerUrl, Map<String, String> requestTags) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                                                            .GET()
                                                            .uri(controllerUrl.toURI())
                                                            .timeout(Constants.DEFAULT_CONNECT_TIMEOUT);
            requestTags.forEach(requestBuilder::header);
            HttpResponse<String> response = getSimpleHttpClient().send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() / 100 != 2) {
                throw new CloudException(
                    MessageFormat.format(Messages.CF_ROOT_DOCUMENT_REQUEST_TO_0_RETURNED_1, controllerUrl, response.statusCode()));
            }

            return (Map<String, Object>) JsonUtil.convertJsonToMap(response.body())
                                                 .get(Constants.ROOT_DOCUMENT_LINKS_LIST);
        } catch (InterruptedException e) {
            Thread.currentThread()
                  .interrupt();
            throw new CloudException(MessageFormat.format(Messages.INTERRUPTED_WHILE_CALLING_THE_CF_ROOT_URL_AT_0, controllerUrl), e);
        } catch (Exception e) {
            throw new CloudException(MessageFormat.format(Messages.FAILED_TO_CALL_THE_CF_ROOT_AT_0_WITH_1, controllerUrl, e.getMessage()),
                                     e);
        }
    }

    private static java.net.http.HttpClient getSimpleHttpClient() {
        return java.net.http.HttpClient.newBuilder()
                                       .connectTimeout(Constants.DEFAULT_CONNECT_TIMEOUT)
                                       .build();
    }

    private URL fallbackUrl(URL controllerUrl, String pathSuffix) {
        try {
            return URI.create(controllerUrl.toString() + pathSuffix)
                      .toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(
                MessageFormat.format(Messages.COULD_NOT_RESOLVE_URL_FROM_0_1, controllerUrl, pathSuffix), e);
        }
    }

    private OAuthClient createOAuthClient(URL controllerUrl) {
        return restUtil.createOAuthClientByControllerUrl(controllerUrl, shouldTrustSelfSignedCertificates());
    }

}
