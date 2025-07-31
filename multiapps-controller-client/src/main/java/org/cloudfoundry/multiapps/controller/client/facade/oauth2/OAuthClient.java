package org.cloudfoundry.multiapps.controller.client.facade.oauth2;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.client.facade.Constants;
import org.cloudfoundry.multiapps.controller.client.facade.util.JsonUtil;
import org.cloudfoundry.reactor.TokenProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import org.cloudfoundry.multiapps.controller.client.facade.CloudCredentials;
import org.cloudfoundry.multiapps.controller.client.facade.adapters.OAuthTokenProvider;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

/**
 * Client that can handle authentication against a UAA instance
 */
public class OAuthClient {

    private static final long MAX_RETRY_ATTEMPTS = 3;
    private static final Duration RETRY_INTERVAL = Duration.ofSeconds(3);

    private final URL authorizationUrl;
    protected OAuth2AccessTokenWithAdditionalInfo token;
    protected CloudCredentials credentials;
    protected final WebClient webClient;
    protected final TokenFactory tokenFactory;

    public OAuthClient(URL authorizationUrl, WebClient webClient) {
        this.authorizationUrl = authorizationUrl;
        this.webClient = webClient;
        this.tokenFactory = new TokenFactory();
    }

    public void init(CloudCredentials credentials) {
        if (credentials != null) {
            this.credentials = credentials;
            if (credentials.getToken() != null) {
                this.token = credentials.getToken();
            } else {
                this.token = createToken();
            }
        }
    }

    public void clear() {
        this.token = null;
        this.credentials = null;
    }

    public OAuth2AccessTokenWithAdditionalInfo getToken() {
        if (token == null) {
            return null;
        }
        if (shouldRefreshToken()) {
            token = createToken();
        }
        return token;
    }

    public String getAuthorizationHeaderValue() {
        OAuth2AccessTokenWithAdditionalInfo accessToken = getToken();
        if (accessToken != null) {
            return accessToken.getAuthorizationHeaderValue();
        }
        return null;
    }

    public TokenProvider getTokenProvider() {
        return new OAuthTokenProvider(this);
    }

    private boolean shouldRefreshToken() {
        return credentials.isRefreshable() && token.getOAuth2AccessToken()
                                                   .getExpiresAt()
                                                   .isBefore(Instant.now()
                                                                    .plus(50, ChronoUnit.SECONDS));
    }

    protected OAuth2AccessTokenWithAdditionalInfo createToken() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", credentials.getClientId());
        formData.add("client_secret", credentials.getClientSecret());
        formData.add("username", credentials.getEmail());
        formData.add("password", credentials.getPassword());
        formData.add("response_type", "token");
        addLoginHintIfPresent(formData);

        Oauth2AccessTokenResponse oauth2AccessTokenResponse = fetchOauth2AccessToken(formData);
        return tokenFactory.createToken(oauth2AccessTokenResponse);
    }

    private void addLoginHintIfPresent(MultiValueMap<String, String> formData) {
        if (StringUtils.hasLength(credentials.getOrigin())) {
            Map<String, String> loginHintMap = new HashMap<>();
            loginHintMap.put(Constants.ORIGIN_KEY, credentials.getOrigin());
            formData.add("login_hint", JsonUtil.convertToJson(loginHintMap));
        }
    }

    private Oauth2AccessTokenResponse fetchOauth2AccessToken(MultiValueMap<String, String> formData) {
        try {
            return webClient.post()
                            .uri(authorizationUrl + "/oauth/token")
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                            .body(BodyInserters.fromFormData(formData))
                            .retrieve()
                            .bodyToFlux(Oauth2AccessTokenResponse.class)
                            .retryWhen(Retry.fixedDelay(MAX_RETRY_ATTEMPTS, RETRY_INTERVAL)
                                            .onRetryExhaustedThrow(this::throwOriginalError))
                            .blockFirst();
        } catch (WebClientResponseException e) {
            throw new ResponseStatusException(e.getStatusCode(), e.getMessage(), e);
        }
    }

    private Throwable throwOriginalError(RetryBackoffSpec retrySpec, Retry.RetrySignal signal) {
        return signal.failure();
    }

}
