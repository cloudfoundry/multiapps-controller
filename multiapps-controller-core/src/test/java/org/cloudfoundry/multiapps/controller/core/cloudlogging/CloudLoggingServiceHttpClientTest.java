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
import org.junit.jupiter.api.BeforeEach;
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
import reactor.core.publisher.Mono;
import reactor.netty.http.client.PrematureCloseException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class CloudLoggingServiceHttpClientTest {

    private CloudLoggingServiceHttpClient client;

    @BeforeEach
    void setUp() {
        client = new CloudLoggingServiceHttpClient();
    }

    @Test
    void createWebClientWithMtls_failSafeTrue_returnsNullOnInvalidCredentials() {
        LoggingConfiguration config = configBuilder(true).serverCa("not a pem")
                                                         .clientCert("not a pem")
                                                         .clientKey("not a pem")
                                                         .build();

        WebClient webClient = client.createWebClientWithMtls(config);

        assertNull(webClient);
    }

    @Test
    void createWebClientWithMtls_failSafeFalse_throwsOnInvalidCredentials() {
        LoggingConfiguration config = configBuilder(false).serverCa("not a pem")
                                                          .clientCert("not a pem")
                                                          .clientKey("not a pem")
                                                          .build();

        assertThrows(SLException.class, () -> client.createWebClientWithMtls(config));
    }

    @Test
    void sendLogs_2xxResponse_doesNotThrow() {
        WebClient webClient = stubWebClient(req -> response(HttpStatus.OK));

        assertDoesNotThrow(() -> client.sendLogsToCloudLoggingService(configBuilder(true).build(), webClient, sampleBatch()));
    }

    @Test
    void sendLogs_sendsJsonContentTypeHeader() {
        AtomicInteger calls = new AtomicInteger();
        WebClient webClient = stubWebClient(req -> {
            calls.incrementAndGet();
            assertEquals(MediaType.APPLICATION_JSON_VALUE, req.headers()
                                                              .getFirst(HttpHeaders.CONTENT_TYPE));
            return response(HttpStatus.OK);
        });

        client.sendLogsToCloudLoggingService(configBuilder(true).build(), webClient, sampleBatch());

        assertEquals(1, calls.get());
    }

    @Test
    void sendLogs_non2xxNonRetryable_failSafeTrue_doesNotThrow() {
        WebClient webClient = stubWebClient(req -> response(HttpStatus.NOT_FOUND));

        assertDoesNotThrow(() -> client.sendLogsToCloudLoggingService(configBuilder(true).build(), webClient, sampleBatch()));
    }

    @Test
    void sendLogs_non2xxNonRetryable_failSafeFalse_throwsSLException() {
        WebClient webClient = stubWebClient(req -> response(HttpStatus.NOT_FOUND));

        assertThrows(SLException.class,
                     () -> client.sendLogsToCloudLoggingService(configBuilder(false).build(), webClient, sampleBatch()));
    }

    @ParameterizedTest
    @ValueSource(ints = { 408, 425, 429, 500, 502, 503, 504 })
    void sendLogs_retryableStatus_isRetriedUntilSuccess(int retryableStatus) {
        AtomicInteger attempts = new AtomicInteger();
        WebClient webClient = stubWebClient(req -> {
            int n = attempts.incrementAndGet();
            return n == 1 ? response(HttpStatus.valueOf(retryableStatus)) : response(HttpStatus.OK);
        });

        client.sendLogsToCloudLoggingService(configBuilder(true).build(), webClient, sampleBatch());

        assertEquals(2, attempts.get());
    }

    @Test
    void sendLogs_persistentRetryableStatus_failSafeTrue_doesNotThrowAfterExhaustion() {
        AtomicInteger attempts = new AtomicInteger();
        WebClient webClient = stubWebClient(req -> {
            attempts.incrementAndGet();
            return response(HttpStatus.SERVICE_UNAVAILABLE);
        });

        assertDoesNotThrow(() -> client.sendLogsToCloudLoggingService(configBuilder(true).build(), webClient, sampleBatch()));
        assertTrue(attempts.get() >= 2, "expected at least one retry, got " + attempts.get());
    }

    @Test
    void sendLogs_ioExceptionFromExchange_isRetried() {
        AtomicInteger attempts = new AtomicInteger();
        WebClient webClient = stubWebClient(req -> {
            int n = attempts.incrementAndGet();
            return n == 1 ? Mono.error(new IOException("connection reset")) : response(HttpStatus.OK);
        });

        client.sendLogsToCloudLoggingService(configBuilder(true).build(), webClient, sampleBatch());

        assertEquals(2, attempts.get());
    }

    @Test
    void sendLogs_prematureCloseException_isRetried() {
        AtomicInteger attempts = new AtomicInteger();
        WebClient webClient = stubWebClient(req -> {
            int n = attempts.incrementAndGet();
            return n == 1 ? Mono.error(PrematureCloseException.TEST_EXCEPTION) : response(HttpStatus.OK);
        });

        client.sendLogsToCloudLoggingService(configBuilder(true).build(), webClient, sampleBatch());

        assertEquals(2, attempts.get());
    }

    @Test
    void sendLogs_nonRetryableRuntimeException_failSafeTrue_doesNotThrow_andDoesNotRetry() {
        AtomicInteger attempts = new AtomicInteger();
        WebClient webClient = stubWebClient(req -> {
            attempts.incrementAndGet();
            return Mono.error(new IllegalStateException("boom"));
        });

        assertDoesNotThrow(() -> client.sendLogsToCloudLoggingService(configBuilder(true).build(), webClient, sampleBatch()));
        assertEquals(1, attempts.get());
    }

    @Test
    void sendLogs_nonRetryableRuntimeException_failSafeFalse_throwsSLException() {
        WebClient webClient = stubWebClient(req -> Mono.error(new IllegalStateException("boom")));

        assertThrows(SLException.class,
                     () -> client.sendLogsToCloudLoggingService(configBuilder(false).build(), webClient, sampleBatch()));
    }

    @Test
    void sendLogs_cachedClientReusedOnSubsequentCalls() {
        CountingHttpClient countingClient = new CountingHttpClient();
        LoggingConfiguration config = configBuilder(true).build();

        countingClient.sendLogs(config, sampleBatch());
        int creationsAfterFirst = countingClient.clientCreations;
        countingClient.sendLogs(config, sampleBatch());

        assertEquals(creationsAfterFirst, countingClient.clientCreations);
    }

    @Test
    void removeClientFromCache_newClientCreatedOnNextSend() {
        CountingHttpClient countingClient = new CountingHttpClient();
        LoggingConfiguration config = configBuilder(true).build();

        countingClient.sendLogs(config, sampleBatch());
        int creationsAfterFirst = countingClient.clientCreations;

        countingClient.removeClientFromCache(config.getOperationId());
        countingClient.sendLogs(config, sampleBatch());

        assertEquals(creationsAfterFirst + 1, countingClient.clientCreations);
    }

    private static WebClient stubWebClient(Function<ClientRequest, Mono<ClientResponse>> handler) {
        ExchangeFunction exchange = handler::apply;
        return WebClient.builder()
                        .baseUrl("https://cls.example.com")
                        .exchangeFunction(exchange)
                        .build();
    }

    private static Mono<ClientResponse> response(HttpStatus status) {
        return Mono.just(ClientResponse.create(status)
                                       .build());
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

    private static class CountingHttpClient extends CloudLoggingServiceHttpClient {

        int clientCreations = 0;

        @Override
        public WebClient createWebClientWithMtls(LoggingConfiguration loggingConfiguration) {
            clientCreations++;
            return mock(WebClient.class);
        }

        @Override
        public void sendLogsToCloudLoggingService(LoggingConfiguration loggingConfiguration, WebClient webClient,
                                                  List<ExternalOperationLogEntry> logEntryBatch) {
            // no-op: this test only exercises client caching, not the HTTP exchange
        }
    }
}
