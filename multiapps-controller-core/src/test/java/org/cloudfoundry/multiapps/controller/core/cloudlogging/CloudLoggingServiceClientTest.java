package org.cloudfoundry.multiapps.controller.core.cloudlogging;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.persistence.model.ExternalOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableExternalOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableLoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.LogLevel;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.PrematureCloseException;
import reactor.util.retry.Retry;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CloudLoggingServiceClientTest {

    private static final int MAX_RETRY_ATTEMPTS = 4;

    private CloudLoggingServiceClient clientWithStubWebClient(Function<ClientRequest, Mono<ClientResponse>> handler) {
        WebClient webClient = stubWebClient(handler);
        CloudLoggingServiceWebClientFactory factory = config -> webClient;
        CloudLoggingServiceClient c = new CloudLoggingServiceClient(factory, new CloudLoggingServiceWebClientCache());
        c.withRetrySpec(Retry.max(MAX_RETRY_ATTEMPTS)
                             .filter(c::isRetryableError)
                             .onRetryExhaustedThrow((spec, sig) -> sig.failure()));
        return c;
    }

    @Test
    void sendLogs_2xxResponse_doesNotThrow() {
        CloudLoggingServiceClient client = clientWithStubWebClient(req -> response(HttpStatus.OK));

        assertDoesNotThrow(() -> client.sendLogsToCloudLoggingService(configBuilder(true).build(), sampleBatch()));
    }

    @Test
    void sendLogs_sendsJsonContentTypeHeader() {
        AtomicInteger calls = new AtomicInteger();
        CloudLoggingServiceClient client = clientWithStubWebClient(req -> {
            calls.incrementAndGet();
            assertEquals(MediaType.APPLICATION_JSON_VALUE, req.headers()
                                                              .getFirst(HttpHeaders.CONTENT_TYPE));
            return response(HttpStatus.OK);
        });

        client.sendLogsToCloudLoggingService(configBuilder(true).build(), sampleBatch());

        assertEquals(1, calls.get());
    }

    @Test
    void sendLogs_non2xxNonRetryable_failSafeTrue_doesNotThrow() {
        CloudLoggingServiceClient client = clientWithStubWebClient(req -> response(HttpStatus.NOT_FOUND));

        assertDoesNotThrow(() -> client.sendLogsToCloudLoggingService(configBuilder(true).build(), sampleBatch()));
    }

    @Test
    void sendLogs_non2xxNonRetryable_failSafeFalse_throwsSLException() {
        CloudLoggingServiceClient client = clientWithStubWebClient(req -> response(HttpStatus.NOT_FOUND));

        assertThrows(SLException.class,
                     () -> client.sendLogsToCloudLoggingService(configBuilder(false).build(), sampleBatch()));
    }

    @ParameterizedTest
    @ValueSource(ints = { 408, 425, 429, 500, 502, 503, 504 })
    void sendLogs_retryableStatus_isRetriedUntilSuccess(int retryableStatus) {
        AtomicInteger attempts = new AtomicInteger();
        CloudLoggingServiceClient client = clientWithStubWebClient(req -> {
            int n = attempts.incrementAndGet();
            return n == 1 ? response(HttpStatus.valueOf(retryableStatus)) : response(HttpStatus.OK);
        });

        client.sendLogsToCloudLoggingService(configBuilder(true).build(), sampleBatch());

        assertEquals(2, attempts.get());
    }

    @Test
    void sendLogs_persistentRetryableStatus_failSafeTrue_doesNotThrowAfterExhaustion() {
        AtomicInteger attempts = new AtomicInteger();
        CloudLoggingServiceClient client = clientWithStubWebClient(req -> {
            attempts.incrementAndGet();
            return response(HttpStatus.SERVICE_UNAVAILABLE);
        });

        assertDoesNotThrow(() -> client.sendLogsToCloudLoggingService(configBuilder(true).build(), sampleBatch()));
        assertTrue(attempts.get() >= 2, "expected at least one retry, got " + attempts.get());
    }

    @Test
    void sendLogs_ioExceptionFromExchange_isRetried() {
        AtomicInteger attempts = new AtomicInteger();
        CloudLoggingServiceClient client = clientWithStubWebClient(req -> {
            int n = attempts.incrementAndGet();
            return n == 1 ? Mono.error(new IOException("connection reset")) : response(HttpStatus.OK);
        });

        client.sendLogsToCloudLoggingService(configBuilder(true).build(), sampleBatch());

        assertEquals(2, attempts.get());
    }

    @Test
    void sendLogs_prematureCloseException_isRetried() {
        AtomicInteger attempts = new AtomicInteger();
        CloudLoggingServiceClient client = clientWithStubWebClient(req -> {
            int n = attempts.incrementAndGet();
            return n == 1 ? Mono.error(PrematureCloseException.TEST_EXCEPTION) : response(HttpStatus.OK);
        });

        client.sendLogsToCloudLoggingService(configBuilder(true).build(), sampleBatch());

        assertEquals(2, attempts.get());
    }

    @Test
    void sendLogs_nonRetryableRuntimeException_failSafeTrue_doesNotThrow_andDoesNotRetry() {
        AtomicInteger attempts = new AtomicInteger();
        CloudLoggingServiceClient client = clientWithStubWebClient(req -> {
            attempts.incrementAndGet();
            return Mono.error(new IllegalStateException("boom"));
        });

        assertDoesNotThrow(() -> client.sendLogsToCloudLoggingService(configBuilder(true).build(), sampleBatch()));
        assertEquals(1, attempts.get());
    }

    @Test
    void sendLogs_nonRetryableRuntimeException_failSafeFalse_throwsSLException() {
        CloudLoggingServiceClient client = clientWithStubWebClient(req -> Mono.error(new IllegalStateException("boom")));

        assertThrows(SLException.class,
                     () -> client.sendLogsToCloudLoggingService(configBuilder(false).build(), sampleBatch()));
    }

    @Test
    void sendLogs_cachedClientReusedOnSubsequentCalls() {
        AtomicInteger creations = new AtomicInteger();
        CloudLoggingServiceWebClientFactory countingFactory = config -> {
            creations.incrementAndGet();
            return stubWebClient(req -> response(HttpStatus.OK));
        };
        CloudLoggingServiceClient client = new CloudLoggingServiceClient(countingFactory, new CloudLoggingServiceWebClientCache());
        LoggingConfiguration config = configBuilder(true).build();

        client.sendLogsToCloudLoggingService(config, sampleBatch());
        int afterFirst = creations.get();
        client.sendLogsToCloudLoggingService(config, sampleBatch());

        assertEquals(afterFirst, creations.get());
    }

    @Test
    void removeClientFromCache_newClientCreatedOnNextSend() {
        AtomicInteger creations = new AtomicInteger();
        CloudLoggingServiceWebClientFactory countingFactory = config -> {
            creations.incrementAndGet();
            return stubWebClient(req -> response(HttpStatus.OK));
        };
        CloudLoggingServiceClient client = new CloudLoggingServiceClient(countingFactory, new CloudLoggingServiceWebClientCache());
        LoggingConfiguration config = configBuilder(true).build();

        client.sendLogsToCloudLoggingService(config, sampleBatch());
        int afterFirst = creations.get();

        client.removeClientFromCache(config.getOperationId());
        client.sendLogsToCloudLoggingService(config, sampleBatch());

        assertEquals(afterFirst + 1, creations.get());
    }

    private static WebClient stubWebClient(Function<ClientRequest, Mono<ClientResponse>> handler) {
        ExchangeFunction exchange = handler::apply;
        return WebClient.builder()
                        .baseUrl("https://cls.example.com")
                        .exchangeFunction(exchange)
                        .build();
    }

    private static Mono<ClientResponse> response(HttpStatus status) {
        if (status.is2xxSuccessful()) {
            ClientResponse mock = mock(ClientResponse.class);
            when(mock.statusCode()).thenReturn(status);
            when(mock.headers()).thenReturn(mock(ClientResponse.Headers.class));
            when(mock.releaseBody()).thenReturn(Mono.empty());
            when(mock.bodyToMono(Void.class)).thenReturn(Mono.empty());
            when(mock.toBodilessEntity()).thenReturn(Mono.just(org.springframework.http.ResponseEntity.ok()
                                                                                                       .build()));
            return Mono.just(mock);
        }
        return Mono.error(WebClientResponseException.create(status.value(), status.getReasonPhrase(),
                                                            HttpHeaders.EMPTY, new byte[0], null));
    }

    private static List<ExternalOperationLogEntry> sampleBatch() {
        return List.of(ImmutableExternalOperationLogEntry.builder()
                                                         .id("id-1")
                                                         .timestamp("2024-01-15T10:30:00Z")
                                                         .message("hello")
                                                         .operationLogName("svc")
                                                         .correlationId("op-1")
                                                         .level(LogLevel.INFO.name())
                                                         .build());
    }

    private static ImmutableLoggingConfiguration.Builder configBuilder(boolean failSafe) {
        return ImmutableLoggingConfiguration.builder()
                                            .operationId("op-1")
                                            .mtaSpaceId("space-1")
                                            .logLevel(LogLevel.INFO)
                                            .isFailSafe(failSafe)
                                            .endpointUrl("https://cls.example.com")
                                            .serverCa("server-ca")
                                            .clientCert("client-cert")
                                            .clientKey("client-key");
    }
}
