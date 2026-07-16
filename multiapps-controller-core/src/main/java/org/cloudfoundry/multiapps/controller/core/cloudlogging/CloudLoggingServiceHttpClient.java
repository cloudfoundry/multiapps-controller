package org.cloudfoundry.multiapps.controller.core.cloudlogging;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import jakarta.inject.Inject;
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
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Exceptions;
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

    private final CloudLoggingServiceWebClientFactory webClientFactory;
    private final CloudLoggingServiceWebClientCache webClientCache;
    private Retry retrySpec;

    public CloudLoggingServiceHttpClient() {
        this(new CloudLoggingServiceWebClientFactory(), new CloudLoggingServiceWebClientCache());
    }

    CloudLoggingServiceHttpClient withRetrySpec(Retry retrySpec) {
        this.retrySpec = retrySpec;
        return this;
    }

    @Inject
    public CloudLoggingServiceHttpClient(CloudLoggingServiceWebClientFactory webClientFactory,
                                         CloudLoggingServiceWebClientCache webClientCache) {
        this.webClientFactory = webClientFactory;
        this.webClientCache = webClientCache;
    }

    public void sendLogs(LoggingConfiguration loggingConfiguration, List<ExternalOperationLogEntry> logEntryBatch) {
        WebClient webClient = webClientCache.getOrCreate(loggingConfiguration, this::createWebClientWithMtls);
        sendLogsToCloudLoggingService(loggingConfiguration, webClient, logEntryBatch);
    }

    public void removeClientFromCache(String operationId) {
        webClientCache.remove(operationId);
    }

    public void sendLogsToCloudLoggingService(LoggingConfiguration loggingConfiguration, WebClient webClient,
                                              List<ExternalOperationLogEntry> logEntryBatch) {
        try {
            ResponseEntity<Void> response = executeSendLogHttpRequest(webClient, logEntryBatch);
            if (hasRequestFailed(response)) {
                CloudLoggingServiceUtil.logErrorOrThrowExceptionBasedOnFailSafe(loggingConfiguration, LOGGER,
                                                                                Messages.FAILED_TO_SEND_LOG_MESSAGE_TO_CLS);
            }
        } catch (IllegalStateException | WebClientResponseException e) {
            handleSendLogFailure(loggingConfiguration, e);
        } catch (WebClientException e) {
            if (isTransportFailure(Exceptions.unwrap(e))) {
                handleSendLogFailure(loggingConfiguration, e);
            } else {
                throw e;
            }
        }
    }

    private boolean isTransportFailure(Throwable throwable) {
        return throwable instanceof IOException || throwable instanceof TimeoutException;
    }

    private void handleSendLogFailure(LoggingConfiguration loggingConfiguration, Throwable failure) {
        CloudLoggingServiceUtil.logErrorOrThrowExceptionBasedOnFailSafe(loggingConfiguration, LOGGER,
                                                                        Messages.FAILED_TO_SEND_LOG_MESSAGE_TO_CLS + ": "
                                                                            + describeFailure(failure));
    }

    public WebClient createWebClientWithMtls(LoggingConfiguration loggingConfiguration) {
        return webClientFactory.createWebClientWithMtls(loggingConfiguration);
    }

    private ResponseEntity<Void> executeSendLogHttpRequest(WebClient webClient, List<ExternalOperationLogEntry> logEntryBatch) {
        return webClient.post()
                        .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .bodyValue(JsonUtil.toJson(logEntryBatch))
                        .retrieve()
                        .toBodilessEntity()
                        .timeout(REQUEST_TIMEOUT)
                        .retryWhen(retrySpec != null ? retrySpec : buildRetrySpec())
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

    boolean isRetryableError(Throwable throwable) {
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
