package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * Builds the in-house {@link CloudControllerRestClient} (and the auxiliary {@link CloudSpaceClient} / {@link LogCacheClient}) that talk to
 * the Cloud Controller v3 REST API directly through a blocking Spring {@link RestClient}, with no dependency on the OSS cf-java-client.
 */
@Value.Immutable
public abstract class CloudControllerRestClientFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudControllerRestClientFactory.class);

    private final RestUtil restUtil = new RestUtil();
    // Per-host transport cache — mirrors the OSS connectionContextCache. One shared reactor-netty HttpClient (pool + TLS) and one root-link
    // discovery per Cloud Controller host, reused across the CF / space / log-cache clients. The per-call OAuth token and request tags are
    // NOT cached here; they are applied per RestClient/WebClient build (cheap), so caching the transport is safe.
    private final Map<String, HostTransport> hostTransports = new ConcurrentHashMap<>();

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
        HostTransport transport = hostTransport(controllerUrl, requestTags);
        URL v3ApiUrl = transport.v3ApiUrl();
        URL origin = toOrigin(v3ApiUrl);
        HttpClient httpClient = transport.httpClient();
        RestClient restClient = CloudControllerRestClientBuilder.buildRestClient(httpClient, origin, oAuthClient, requestTags);
        WebClient webClient = CloudControllerRestClientBuilder.buildWebClient(httpClient, origin, oAuthClient, requestTags);
        CloudControllerV3Client cc = new CloudControllerV3Client(restClient, webClient);
        // Per-upload client factory: the app-bits upload (POST /v3/packages/{guid}/upload) must be bounded by the caller's
        // per-MTA uploadTimeout, not the shared 900s response timeout. Build a dedicated RestClient whose response timeout is
        // that uploadTimeout, mirroring the OSS startUpload's .timeout(uploadTimeout). Everything else (pool, connect, TLS) matches.
        java.util.function.Function<Duration, RestClient> uploadRestClientFactory = uploadTimeout -> {
            // Upload uses a one-off client with only its response timeout overridden; no dedicated event-loop pool (threadPoolSize empty)
            // so it does not spin up its own LoopResources.
            var uploadOptions = new CloudControllerRestClientBuilder.TransportOptions(getConnectTimeout(), Optional.of(uploadTimeout),
                                                                                      getSslHandshakeTimeout(), getConnectionPoolSize(),
                                                                                      Optional.empty(),
                                                                                      shouldTrustSelfSignedCertificates());
            return CloudControllerRestClientBuilder.build(origin, oAuthClient, requestTags, uploadOptions);
        };
        return new CloudControllerRestClientV3Impl(v3ApiUrl, oAuthClient, target, restClient, cc, uploadRestClientFactory);
    }

    public CloudSpaceClient createSpaceClient(URL controllerUrl, OAuthClient oAuthClient, Map<String, String> requestTags) {
        HostTransport transport = hostTransport(controllerUrl, requestTags);
        URL origin = toOrigin(transport.v3ApiUrl());
        RestClient restClient = CloudControllerRestClientBuilder.buildRestClient(transport.httpClient(), origin, oAuthClient, requestTags);
        WebClient webClient = CloudControllerRestClientBuilder.buildWebClient(transport.httpClient(), origin, oAuthClient, requestTags);
        return new CloudSpaceClient(new CloudControllerV3Client(restClient, webClient));
    }

    public LogCacheClient createLogCacheClient(URL controllerUrl, OAuthClient oAuthClient, Map<String, String> requestTags) {
        HostTransport transport = hostTransport(controllerUrl, requestTags);
        RestClient restClient = CloudControllerRestClientBuilder.buildRestClient(transport.httpClient(), transport.logCacheUrl(),
                                                                                 oAuthClient, requestTags);
        return new LogCacheClient(restClient);
    }

    // Get (or lazily build) the shared per-host transport: one reactor-netty HttpClient + the resolved v3 / log-cache URLs, discovered
    // once per host. Keyed on the controller host so the CF / space / log-cache clients for the same landscape share the pool.
    private HostTransport hostTransport(URL controllerUrl, Map<String, String> requestTags) {
        return hostTransports.computeIfAbsent(controllerUrl.getHost(), host -> {
            HttpClient httpClient = CloudControllerRestClientBuilder.buildHttpClient(transportOptions());
            URL v3ApiUrl = deriveV3ApiUrl(controllerUrl, requestTags);
            URL logCacheUrl = deriveLogCacheUrl(controllerUrl, requestTags);
            return new HostTransport(httpClient, v3ApiUrl, logCacheUrl);
        });
    }

    private CloudControllerRestClientBuilder.TransportOptions transportOptions() {
        return new CloudControllerRestClientBuilder.TransportOptions(getConnectTimeout(), getResponseTimeout(), getSslHandshakeTimeout(),
                                                                     getConnectionPoolSize(), getThreadPoolSize(),
                                                                     shouldTrustSelfSignedCertificates());
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

    private static java.net.http.HttpClient rootHttpClient() {
        return java.net.http.HttpClient.newBuilder()
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

    // The cached, host-scoped transport: one shared reactor-netty HttpClient (pool + TLS + proxy) plus the CF root-link discovery results
    // (v3 API + log-cache base URLs), resolved once per host.
    private record HostTransport(HttpClient httpClient, URL v3ApiUrl, URL logCacheUrl) {
    }

}
