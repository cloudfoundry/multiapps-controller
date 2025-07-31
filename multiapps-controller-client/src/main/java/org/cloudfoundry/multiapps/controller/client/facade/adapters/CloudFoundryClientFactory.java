package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v3.organizations.OrganizationsV3;
import org.cloudfoundry.client.v3.spaces.SpacesV3;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.client.v3.organizations.ReactorOrganizationsV3;
import org.cloudfoundry.reactor.client.v3.spaces.ReactorSpacesV3;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import org.cloudfoundry.multiapps.controller.client.facade.CloudException;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.oauth2.OAuthClient;
import org.cloudfoundry.multiapps.controller.client.facade.rest.CloudSpaceClient;
import org.cloudfoundry.multiapps.controller.client.facade.util.CloudUtil;
import org.cloudfoundry.multiapps.controller.client.facade.util.JsonUtil;

import reactor.core.publisher.Mono;

@Value.Immutable
public abstract class CloudFoundryClientFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudFoundryClientFactory.class);

    static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
                                                    .executor(Executors.newSingleThreadExecutor())
                                                    .followRedirects(HttpClient.Redirect.NORMAL)
                                                    .connectTimeout(Duration.ofMinutes(10))
                                                    .build();

    private final Map<String, ConnectionContext> connectionContextCache = new ConcurrentHashMap<>();

    public abstract Optional<Duration> getSslHandshakeTimeout();

    public abstract Optional<Duration> getConnectTimeout();

    public abstract Optional<Integer> getConnectionPoolSize();

    public abstract Optional<Integer> getThreadPoolSize();

    public abstract Optional<Duration> getResponseTimeout();

    public CloudFoundryClient createClient(URL controllerUrl, OAuthClient oAuthClient, Map<String, String> requestTags) {
        return ReactorCloudFoundryClient.builder()
                                        .connectionContext(getOrCreateConnectionContext(controllerUrl.getHost()))
                                        .tokenProvider(oAuthClient.getTokenProvider())
                                        .requestTags(requestTags)
                                        .build();
    }

    public LogCacheClient createLogCacheClient(URL controllerUrl, OAuthClient oAuthClient, Map<String, String> requestTags) {
        return new LogCacheClient(oAuthClient, requestTags, getOrCreateConnectionContext(controllerUrl.getHost()));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callCfRoot(URL controllerUrl, Map<String, String> requestTags) {
        HttpResponse<String> response;
        try {
            HttpRequest request = buildCfRootRequest(controllerUrl, requestTags);
            LOGGER.info(MessageFormat.format(Messages.CALLING_CF_ROOT_0_TO_ACCESS_LOG_CACHE_URL, controllerUrl));
            response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                var status = HttpStatus.valueOf(response.statusCode());
                throw new CloudOperationException(status, status.getReasonPhrase(), response.body());
            }
            LOGGER.info(Messages.CF_ROOT_REQUEST_FINISHED);
        } catch (InterruptedException | URISyntaxException | IOException e) {
            throw new CloudException(e.getMessage(), e);
        }
        var map = JsonUtil.convertJsonToMap(response.body());
        return (Map<String, Object>) map.get("links");
    }

    private HttpRequest buildCfRootRequest(URL controllerUrl, Map<String, String> requestTags) throws URISyntaxException {
        var requestBuilder = HttpRequest.newBuilder()
                                        .GET()
                                        .uri(controllerUrl.toURI())
                                        .timeout(Duration.ofMinutes(5));
        requestTags.forEach(requestBuilder::header);
        return requestBuilder.build();
    }

    public CloudSpaceClient createSpaceClient(URL controllerUrl, OAuthClient oAuthClient, Map<String, String> requestTags) {
        String v3Api;
        try {
            var links = CloudUtil.executeWithRetry(() -> callCfRoot(controllerUrl, requestTags));
            @SuppressWarnings("unchecked")
            var ccv3 = (Map<String, Object>) links.get("cloud_controller_v3");
            v3Api = (String) ccv3.get("href");
        } catch (CloudException e) {
            LOGGER.warn(MessageFormat.format(Messages.CALL_TO_0_FAILED_WITH_1, controllerUrl.toString(), e.getMessage()), e);
            v3Api = controllerUrl + "/v3";
        }
        var spacesV3 = createV3SpacesClient(controllerUrl, v3Api, oAuthClient, requestTags);
        var orgsV3 = createV3OrgsClient(controllerUrl, v3Api, oAuthClient, requestTags);
        return new CloudSpaceClient(spacesV3, orgsV3);
    }

    private SpacesV3 createV3SpacesClient(URL controllerUrl, String v3Api, OAuthClient oAuthClient, Map<String, String> requestTags) {
        return new ReactorSpacesV3(getOrCreateConnectionContext(controllerUrl.getHost()),
                                   Mono.just(v3Api),
                                   oAuthClient.getTokenProvider(),
                                   requestTags);
    }

    private OrganizationsV3 createV3OrgsClient(URL controllerUrl, String v3Api, OAuthClient oAuthClient, Map<String, String> requestTags) {
        return new ReactorOrganizationsV3(getOrCreateConnectionContext(controllerUrl.getHost()),
                                          Mono.just(v3Api),
                                          oAuthClient.getTokenProvider(),
                                          requestTags);
    }

    public ConnectionContext getOrCreateConnectionContext(String controllerApiHost) {
        return connectionContextCache.computeIfAbsent(controllerApiHost, this::createConnectionContext);
    }

    private ConnectionContext createConnectionContext(String controllerApiHost) {
        DefaultConnectionContext.Builder builder = DefaultConnectionContext.builder()
                                                                           .apiHost(controllerApiHost);
        getSslHandshakeTimeout().ifPresent(builder::sslHandshakeTimeout);
        getConnectTimeout().ifPresent(builder::connectTimeout);
        getConnectionPoolSize().ifPresent(builder::connectionPoolSize);
        getThreadPoolSize().ifPresent(builder::threadPoolSize);
        builder.additionalHttpClientConfiguration(this::getAdditionalHttpClientConfiguration);
        return builder.build();
    }

    private reactor.netty.http.client.HttpClient getAdditionalHttpClientConfiguration(reactor.netty.http.client.HttpClient client) {
        var clientWithOptions = client;
        if (getResponseTimeout().isPresent()) {
            clientWithOptions = clientWithOptions.responseTimeout(getResponseTimeout().get());
        }

        return clientWithOptions;
    }
}
