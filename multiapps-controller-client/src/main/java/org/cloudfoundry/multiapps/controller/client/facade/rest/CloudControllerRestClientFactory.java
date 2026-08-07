package org.cloudfoundry.multiapps.controller.client.facade.rest;

import org.cloudfoundry.multiapps.controller.client.facade.CloudCredentials;
import org.cloudfoundry.multiapps.controller.client.facade.CloudException;
import org.cloudfoundry.multiapps.controller.client.facade.adapters.LogCacheClient;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.oauth2.OAuthClient;
import org.cloudfoundry.multiapps.controller.client.facade.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.facade.util.RestUtil;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Builds the in-house {@link CloudControllerRestClient} (and the auxiliary {@link CloudSpaceClient} / {@link LogCacheClient}) that talk to
 * the Cloud Controller v3 REST API directly through a blocking Spring {@link RestClient}, with no dependency on the OSS cf-java-client.
 */
@Value.Immutable
public abstract class CloudControllerRestClientFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudControllerRestClientFactory.class);

    private final RestUtil restUtil = new RestUtil();

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
        return createClient(controllerUrl, credentials, target, createOAuthClient(controllerUrl, credentials.getOrigin()),
                            Collections.emptyMap());
    }

    public CloudControllerRestClient createClient(URL controllerUrl, CloudCredentials credentials, CloudSpace target,
                                                  OAuthClient oAuthClient, Map<String, String> requestTags) {
        oAuthClient.init(credentials);
        URL v3ApiUrl = deriveV3ApiUrl(controllerUrl, requestTags);
        RestClient restClient = buildRestClient(toOrigin(v3ApiUrl), oAuthClient, requestTags);
        return new CloudControllerRestClientV3Impl(v3ApiUrl, oAuthClient, target, restClient);
    }

    public CloudSpaceClient createSpaceClient(URL controllerUrl, OAuthClient oAuthClient, Map<String, String> requestTags) {
        URL v3ApiUrl = deriveV3ApiUrl(controllerUrl, requestTags);
        RestClient restClient = buildRestClient(toOrigin(v3ApiUrl), oAuthClient, requestTags);
        return new CloudSpaceClient(new CloudControllerV3Client(restClient));
    }

    public LogCacheClient createLogCacheClient(URL controllerUrl, OAuthClient oAuthClient, Map<String, String> requestTags) {
        URL logCacheUrl = deriveLogCacheUrl(controllerUrl, requestTags);
        RestClient restClient = buildRestClient(logCacheUrl, oAuthClient, requestTags);
        return new LogCacheClient(restClient);
    }

    private RestClient buildRestClient(URL baseUrl, OAuthClient oAuthClient, Map<String, String> requestTags) {
        var transportOptions = new CloudControllerRestClientBuilder.TransportOptions(getConnectTimeout(), getResponseTimeout(),
                                                                                     getConnectionPoolSize(),
                                                                                     shouldTrustSelfSignedCertificates());
        return CloudControllerRestClientBuilder.build(baseUrl, oAuthClient, requestTags, transportOptions);
    }

    // scheme://host[:port] of the given URL, dropping any path (e.g. the trailing /v3). The RestClient baseUrl must be the origin only,
    // because every operation path already carries the "/v3/..." prefix.
    private URL toOrigin(URL url) {
        try {
            int port = url.getPort();
            String origin = url.getProtocol() + "://" + url.getHost() + (port == -1 ? "" : ":" + port);
            return URI.create(origin)
                      .toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Could not derive the API origin from " + url, e);
        }
    }

    // Resolve the CF v3 API base URL from the CF root "cloud_controller_v3" link; fall back to controllerUrl + "/v3".
    @SuppressWarnings("unchecked")
    private URL deriveV3ApiUrl(URL controllerUrl, Map<String, String> requestTags) {
        try {
            Map<String, Object> links = fetchRootLinks(controllerUrl, requestTags);
            Map<String, Object> ccv3 = (Map<String, Object>) links.get("cloud_controller_v3");
            return URI.create((String) ccv3.get("href"))
                      .toURL();
        } catch (Exception e) {
            return fallbackUrl(controllerUrl, "/v3");
        }
    }

    // Log-Cache base URL from the CF root "log_cache" link; fall back to the "log-cache" subdomain of the controller host.
    @SuppressWarnings("unchecked")
    private URL deriveLogCacheUrl(URL controllerUrl, Map<String, String> requestTags) {
        try {
            Map<String, Object> links = fetchRootLinks(controllerUrl, requestTags);
            Map<String, Object> logCache = (Map<String, Object>) links.get("log_cache");
            return URI.create((String) logCache.get("href"))
                      .toURL();
        } catch (Exception e) {
            String host = controllerUrl.getHost();
            String logCacheHost = host.startsWith("api.") ? "log-cache." + host.substring("api.".length()) : "log-cache." + host;
            try {
                return URI.create(controllerUrl.getProtocol() + "://" + logCacheHost)
                          .toURL();
            } catch (MalformedURLException me) {
                throw new CloudException("Could not derive the log-cache URL from " + controllerUrl, me);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchRootLinks(URL controllerUrl, Map<String, String> requestTags) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                                                            .GET()
                                                            .uri(controllerUrl.toURI())
                                                            .timeout(Duration.ofMinutes(1));
            requestTags.forEach(requestBuilder::header);
            HttpResponse<String> response = rootHttpClient().send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new CloudException("CF root request to " + controllerUrl + " returned " + response.statusCode());
            }
            return (Map<String, Object>) JsonUtil.convertJsonToMap(response.body())
                                                 .get("links");
        } catch (InterruptedException e) {
            Thread.currentThread()
                  .interrupt();
            throw new CloudException("Interrupted while calling the CF root at " + controllerUrl, e);
        } catch (Exception e) {
            throw new CloudException("Failed to call the CF root at " + controllerUrl + ": " + e.getMessage(), e);
        }
    }

    private static HttpClient rootHttpClient() {
        return HttpClient.newBuilder()
                         .connectTimeout(Duration.ofMinutes(1))
                         .build();
    }

    private URL fallbackUrl(URL controllerUrl, String pathSuffix) {
        try {
            return URI.create(controllerUrl.toString() + pathSuffix)
                      .toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Could not derive URL from " + controllerUrl + pathSuffix, e);
        }
    }

    private OAuthClient createOAuthClient(URL controllerUrl, String origin) {
        return restUtil.createOAuthClientByControllerUrl(controllerUrl, shouldTrustSelfSignedCertificates());
    }

}
