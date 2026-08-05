package org.cloudfoundry.multiapps.controller.client.facade.rest;

import org.cloudfoundry.multiapps.controller.client.facade.CloudCredentials;
import org.cloudfoundry.multiapps.controller.client.facade.adapters.CloudFoundryClientFactory;
import org.cloudfoundry.multiapps.controller.client.facade.adapters.ImmutableCloudFoundryClientFactory;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.oauth2.OAuthClient;
import org.cloudfoundry.multiapps.controller.client.facade.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.facade.util.RestUtil;
import org.cloudfoundry.client.CloudFoundryClient;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.net.URL;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

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

    /**
     * Feature flag for the cf-java-client migration PoC. When {@code true}, {@link #createClient} builds the in-house
     * {@link CloudControllerRestClientV3Impl} (direct CF v3 REST, no cf-java-client); when {@code false} (default), it builds the
     * OSS-backed {@link CloudControllerRestClientImpl}. Kept default-off so existing behaviour is unchanged and the two can be A/B
     * compared against the same live Cloud Foundry.
     */
    @Value.Default
    public boolean useV3RestClient() {
        // cf_java_client_poc branch: default ON so the in-house CloudControllerRestClientV3Impl is always chosen for OQ verification.
        // Do NOT merge this default to main — the switch belongs off there until the client is validated.
        return true;
    }

    @Value.Derived
    public CloudFoundryClientFactory getCloudFoundryClientFactory() {
        ImmutableCloudFoundryClientFactory.Builder builder = ImmutableCloudFoundryClientFactory.builder();
        getSslHandshakeTimeout().ifPresent(builder::sslHandshakeTimeout);
        getConnectTimeout().ifPresent(builder::connectTimeout);
        getConnectionPoolSize().ifPresent(builder::connectionPoolSize);
        getThreadPoolSize().ifPresent(builder::threadPoolSize);
        getResponseTimeout().ifPresent(builder::responseTimeout);
        return builder.build();
    }

    public CloudControllerRestClient createClient(URL controllerUrl, CloudCredentials credentials, String organizationName,
                                                  String spaceName, OAuthClient oAuthClient, Map<String, String> requestTags) {
        oAuthClient.init(credentials);
        CloudSpaceClient spaceGetter = getCloudFoundryClientFactory().createSpaceClient(controllerUrl, oAuthClient, requestTags);
        CloudSpace target = spaceGetter.getSpace(organizationName, spaceName);
        return createClient(controllerUrl, credentials, target, oAuthClient, requestTags);
    }

    public CloudControllerRestClient createClient(URL controllerUrl, CloudCredentials credentials, CloudSpace target) {
        return createClient(controllerUrl, credentials, target, createOAuthClient(controllerUrl, credentials.getOrigin()),
                            Collections.emptyMap());
    }

    public CloudControllerRestClient createClient(URL controllerUrl, CloudCredentials credentials, CloudSpace target,
                                                  OAuthClient oAuthClient, Map<String, String> requestTags) {
        oAuthClient.init(credentials);
        if (useV3RestClient()) {
            return createV3RestClient(controllerUrl, target, oAuthClient, requestTags);
        }
        CloudFoundryClient delegate = getCloudFoundryClientFactory().createClient(controllerUrl, oAuthClient, requestTags);
        return new CloudControllerRestClientImpl(delegate, target);
    }

    private CloudControllerRestClient createV3RestClient(URL controllerUrl, CloudSpace target, OAuthClient oAuthClient,
                                                         Map<String, String> requestTags) {
        URL v3ApiUrl = deriveV3ApiUrl(controllerUrl, requestTags);
        // The RestClient baseUrl must be the API ORIGIN (scheme://host[:port]) only — every operation path already carries the
        // "/v3/..." prefix, so using the ".../v3" href as the base would produce a duplicated "/v3/v3/apps" path (CF -> 404 Unknown
        // request). We keep v3ApiUrl (which includes /v3) for reference/root-link semantics but strip it to the origin for the base.
        URL baseUrl = toOrigin(v3ApiUrl);
        LOGGER.warn("[cf-v3-poc] controllerUrl={} resolved v3ApiUrl={} restClientBaseUrl={}", controllerUrl, v3ApiUrl, baseUrl);
        var transportOptions = new CloudControllerRestClientBuilder.TransportOptions(getConnectTimeout(), getResponseTimeout(),
                                                                                     getConnectionPoolSize(),
                                                                                     shouldTrustSelfSignedCertificates());
        RestClient restClient = CloudControllerRestClientBuilder.build(baseUrl, oAuthClient, requestTags, transportOptions);
        return new CloudControllerRestClientV3Impl(v3ApiUrl, oAuthClient, target, restClient);
    }

    // scheme://host[:port] of the given URL, dropping any path (e.g. the trailing /v3).
    private URL toOrigin(URL url) {
        try {
            int port = url.getPort();
            String origin = url.getProtocol() + "://" + url.getHost() + (port == -1 ? "" : ":" + port);
            return java.net.URI.create(origin)
                               .toURL();
        } catch (java.net.MalformedURLException e) {
            throw new IllegalArgumentException("Could not derive the API origin from " + url, e);
        }
    }

    // Resolve the CF v3 API base URL the same way the OSS factory does: GET the CF root and read links.cloud_controller_v3.href.
    // The conventional controllerUrl + "/v3" is only a fallback — on some landscapes the real v3 href differs, and using the naive
    // path yields "404 Not Found: Unknown request" from the Cloud Controller.
    @SuppressWarnings("unchecked")
    private URL deriveV3ApiUrl(URL controllerUrl, Map<String, String> requestTags) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                                                            .GET()
                                                            .uri(controllerUrl.toURI())
                                                            .timeout(Duration.ofMinutes(1));
            requestTags.forEach(requestBuilder::header);
            HttpResponse<String> response = rootHttpClient().send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                return fallbackV3ApiUrl(controllerUrl);
            }
            Map<String, Object> links = (Map<String, Object>) JsonUtil.convertJsonToMap(response.body())
                                                                      .get("links");
            Map<String, Object> ccv3 = (Map<String, Object>) links.get("cloud_controller_v3");
            String href = (String) ccv3.get("href");
            return java.net.URI.create(href)
                               .toURL();
        } catch (InterruptedException e) {
            Thread.currentThread()
                  .interrupt();
            return fallbackV3ApiUrl(controllerUrl);
        } catch (Exception e) {
            return fallbackV3ApiUrl(controllerUrl);
        }
    }

    private static java.net.http.HttpClient rootHttpClient() {
        return java.net.http.HttpClient.newBuilder()
                                       .connectTimeout(Duration.ofMinutes(1))
                                       .build();
    }

    private URL fallbackV3ApiUrl(URL controllerUrl) {
        try {
            return java.net.URI.create(controllerUrl.toString() + "/v3")
                               .toURL();
        } catch (java.net.MalformedURLException e) {
            throw new IllegalArgumentException("Could not derive the CF v3 API URL from " + controllerUrl, e);
        }
    }


    private OAuthClient createOAuthClient(URL controllerUrl, String origin) {
        return restUtil.createOAuthClientByControllerUrl(controllerUrl, shouldTrustSelfSignedCertificates());
    }

}
