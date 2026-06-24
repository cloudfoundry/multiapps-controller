package org.cloudfoundry.multiapps.controller.persistence.services;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.model.ExternalOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.util.CloudLoggingServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.PrematureCloseException;
import reactor.util.retry.Retry;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Named("cloudLoggingServiceHttpClient")
public class CloudLoggingServiceHttpClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudLoggingServiceHttpClient.class);
    private static final int MAX_RETRY_ATTEMPTS = 4;
    private static final Duration INITIAL_RETRY_BACKOFF = Duration.ofMillis(500);
    private static final Duration MAX_RETRY_BACKOFF = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final Set<Integer> RETRYABLE_STATUS_CODES = Set.of(408, 425, 429, 500, 502, 503, 504);

    public void sendLogsToCloudLoggingService(LoggingConfiguration loggingConfiguration, WebClient webClient,
                                              List<ExternalOperationLogEntry> logEntryBatch) {
        try {
            ResponseEntity<Void> response = executeSendLogHttpRequest(webClient, logEntryBatch);
            if (hasRequestFailed(response)) {
                CloudLoggingServiceUtil.logErrorOrThrowExceptionBasedOnFailSafe(loggingConfiguration, LOGGER,
                                                                                Messages.FAILED_TO_SEND_LOG_MESSAGE_TO_CLS);
            }
        } catch (RuntimeException e) {
            CloudLoggingServiceUtil.logErrorOrThrowExceptionBasedOnFailSafe(loggingConfiguration, LOGGER,
                                                                            Messages.FAILED_TO_SEND_LOG_MESSAGE_TO_CLS + ": "
                                                                                + describeFailure(e));
        }
    }

    public WebClient createWebClientWithMtls(LoggingConfiguration loggingConfiguration) {
        SslContext sslContext = getSslContext(loggingConfiguration);
        if (sslContext == null) {
            return null;
        }
        HttpClient httpClient = HttpClient.create()
                                          .secure(sslSpec -> sslSpec.sslContext(sslContext));

        return WebClient.builder()
                        .baseUrl(loggingConfiguration.getEndpointUrl())
                        .clientConnector(new ReactorClientHttpConnector(httpClient))
                        .build();
    }

    private SslContext getSslContext(LoggingConfiguration loggingConfiguration) {
        try (InputStream serverCaStream = getCredentialInputStream(loggingConfiguration.getServerCa());
            InputStream clientCertStream = getCredentialInputStream(loggingConfiguration.getClientCert());
            InputStream clientKeyStream = getCredentialInputStream(loggingConfiguration.getClientKey())) {
            return SslContextBuilder.forClient()
                                    .keyManager(clientCertStream, clientKeyStream)
                                    .trustManager(serverCaStream)
                                    .build();
        } catch (IOException | IllegalArgumentException e) {
            // Netty's SslContextBuilder throws IllegalArgumentException for malformed PEM material
            // (e.g. "Input stream not contain valid certificates."). Catch it alongside IOException
            // so cert-format errors honor failSafe instead of bubbling up as an unchecked exception.
            CloudLoggingServiceUtil.logErrorOrThrowExceptionBasedOnFailSafe(loggingConfiguration, LOGGER, e.getMessage());
            return null;
        }
    }

    private InputStream getCredentialInputStream(String credential) {
        return new ByteArrayInputStream((credential.getBytes(StandardCharsets.UTF_8)));
    }

    private ResponseEntity<Void> executeSendLogHttpRequest(WebClient webClient, List<ExternalOperationLogEntry> logEntryBatch) {
        return webClient.post()
                        .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .bodyValue(JsonUtil.toJson(logEntryBatch))
                        .retrieve()
                        .toBodilessEntity()
                        .timeout(REQUEST_TIMEOUT)
                        .retryWhen(buildRetrySpec())
                        .block();
    }

    private Retry buildRetrySpec() {
        return Retry.backoff(MAX_RETRY_ATTEMPTS, INITIAL_RETRY_BACKOFF)
                    .maxBackoff(MAX_RETRY_BACKOFF)
                    .jitter(0.5d)
                    .filter(this::isRetryableError)
                    .doBeforeRetry(retrySignal -> LOGGER.warn(MessageFormat.format(Messages.RETRYING_SEND_LOGS_TO_CLS,
                                                                                   describeFailure(retrySignal.failure()))))
                    .onRetryExhaustedThrow((spec, retrySignal) -> retrySignal.failure());
    }

    private boolean isRetryableError(Throwable throwable) {
        if (throwable instanceof WebClientResponseException responseException) {
            return RETRYABLE_STATUS_CODES.contains(responseException.getStatusCode()
                                                                    .value());
        }
        if (throwable instanceof WebClientRequestException) {
            return true;
        }
        return throwable instanceof PrematureCloseException || throwable instanceof IOException;
    }

    private String describeFailure(Throwable throwable) {
        if (throwable instanceof WebClientResponseException responseException) {
            String retryAfter = responseException.getHeaders()
                                                 .getFirst(HttpHeaders.RETRY_AFTER);
            return MessageFormat.format("HTTP {0} {1}{2}", responseException.getStatusCode()
                                                                            .value(),
                                        responseException.getStatusText(),
                                        retryAfter != null ? " (Retry-After=" + retryAfter + ")" : "");
        }
        return throwable.getClass()
                        .getSimpleName() + ": " + throwable.getMessage();
    }

    private boolean hasRequestFailed(ResponseEntity<Void> response) {
        if (response == null) {
            return true;
        }
        int statusCode = response.getStatusCode()
                                 .value();
        return statusCode < 200 || statusCode > 299;
    }
}
