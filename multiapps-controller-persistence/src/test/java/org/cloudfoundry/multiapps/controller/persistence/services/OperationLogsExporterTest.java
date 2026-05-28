package org.cloudfoundry.multiapps.controller.persistence.services;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.persistence.model.ExternalOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableLoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.LogLevel;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.OperationLogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OperationLogsExporterTest {

    private static final String OPERATION_ID = "op-123";
    private static final String SPACE_ID = "space-1";
    private static final String LOG_DATE = "2024 01 15 10:30:00.000";
    private static final String INFO_LOG = logLine(LOG_DATE, "INFO", "deploy-app.hello-backend",
                                                   "[main] Starting deployment");
    private static final String WARN_LOG = logLine(LOG_DATE, "WARN", "deploy-app.hello-backend",
                                                   "[main] Low memory");
    private static final String ERROR_LOG = logLine(LOG_DATE, "ERROR", "deploy-app.hello-backend",
                                                    "[main] Deployment failed");
    private static final String DEBUG_LOG = logLine(LOG_DATE, "DEBUG", "deploy-app.hello-backend",
                                                    "[main] Debug info");
    private static final String TRACE_LOG = logLine(LOG_DATE, "TRACE", "deploy-app.hello-backend",
                                                    "[main] Trace info");

    @Mock
    private ProcessLogsPersistenceService processLogsPersistenceService;

    private TestOperationLogsExporter exporter;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        exporter = new TestOperationLogsExporter(processLogsPersistenceService);
        exporter.removeClientFromCache(OPERATION_ID);
    }

    @Test
    void testSendLogs_withNullLoggingConfiguration_doesNothing() {
        exporter.sendLogsToCloudLoggingService(null, buildEntry(INFO_LOG));

        assertTrue(exporter.capturedEntries()
                           .isEmpty());
    }

    @Test
    void testSendLogs_withOperationLogEntry_sendsExpectedNumberOfEntries() {
        LoggingConfiguration config = buildConfig(LogLevel.INFO);

        exporter.sendLogsToCloudLoggingService(config, buildEntry(INFO_LOG + WARN_LOG));

        assertEquals(2, exporter.capturedEntries()
                                .size());
    }

    @Test
    void testSendLogs_withOperationLogEntry_setsLevelOnEntry() {
        LoggingConfiguration config = buildConfig(LogLevel.INFO);

        exporter.sendLogsToCloudLoggingService(config, buildEntry(WARN_LOG));

        assertEquals("WARN", exporter.capturedEntries()
                                     .get(0)
                                     .getLevel());
    }

    @Test
    void testSendLogs_withOperationLogEntry_setsCorrelationId() {
        LoggingConfiguration config = buildConfig(LogLevel.INFO);

        exporter.sendLogsToCloudLoggingService(config, buildEntry(INFO_LOG));

        assertEquals(OPERATION_ID, exporter.capturedEntries()
                                           .get(0)
                                           .getCorrelationId());
    }

    @Test
    void testSendLogs_withOperationLogEntry_setsOperationLogName() {
        LoggingConfiguration config = buildConfig(LogLevel.INFO);
        OperationLogEntry entry = ImmutableOperationLogEntry.builder()
                                                            .operationId(OPERATION_ID)
                                                            .operationLog(INFO_LOG)
                                                            .operationLogName("my-log")
                                                            .build();

        exporter.sendLogsToCloudLoggingService(config, entry);

        assertEquals("my-log", exporter.capturedEntries()
                                       .get(0)
                                       .getOperationLogName());
    }

    @Test
    void testSendLogs_withMessageString_extractsLogNameSuffix() {
        LoggingConfiguration config = buildConfig(LogLevel.INFO);

        exporter.sendLogsToCloudLoggingService(config, INFO_LOG);

        assertEquals("hello-backend", exporter.capturedEntries()
                                              .get(0)
                                              .getOperationLogName());
    }

    @Test
    void testSendLogs_withMessageString_setsCorrelationId() {
        LoggingConfiguration config = buildConfig(LogLevel.INFO);

        exporter.sendLogsToCloudLoggingService(config, INFO_LOG);

        assertEquals(OPERATION_ID, exporter.capturedEntries()
                                           .get(0)
                                           .getCorrelationId());
    }

    @Test
    void testSendLogs_withMessageString_setsLevel() {
        LoggingConfiguration config = buildConfig(LogLevel.INFO);

        exporter.sendLogsToCloudLoggingService(config, ERROR_LOG);

        assertEquals("ERROR", exporter.capturedEntries()
                                      .get(0)
                                      .getLevel());
    }

    @Test
    void testSendLogs_withMessageString_producesNoBatchesWhenAllFilteredOut() {
        LoggingConfiguration config = buildConfig(LogLevel.ERROR);

        exporter.sendLogsToCloudLoggingService(config, INFO_LOG + DEBUG_LOG);

        assertTrue(exporter.capturedEntries()
                           .isEmpty());
    }

    static Stream<Arguments> testLogLevelFiltering() {
        String allLevels = INFO_LOG + WARN_LOG + ERROR_LOG + DEBUG_LOG + TRACE_LOG;
        return Stream.of(
            Arguments.of(LogLevel.ERROR, allLevels, 1),
            Arguments.of(LogLevel.WARN, allLevels, 2),
            Arguments.of(LogLevel.INFO, allLevels, 3),
            Arguments.of(LogLevel.DEBUG, allLevels, 4),
            Arguments.of(LogLevel.TRACE, allLevels, 5));
    }

    @ParameterizedTest
    @MethodSource
    void testLogLevelFiltering(LogLevel configuredLevel, String logMessage, int expectedCount) {
        LoggingConfiguration config = buildConfig(configuredLevel);

        exporter.sendLogsToCloudLoggingService(config, buildEntry(logMessage));

        assertEquals(expectedCount, exporter.capturedEntries()
                                            .size());
    }

    @Test
    void testSendLogs_multipleEntriesAreSentInOneBatch() {
        LoggingConfiguration config = buildConfig(LogLevel.INFO);

        exporter.sendLogsToCloudLoggingService(config, buildEntry(INFO_LOG + WARN_LOG + ERROR_LOG));

        assertEquals(1, exporter.capturedBatches.size());
        assertEquals(3, exporter.capturedEntries()
                                .size());
    }

    @Test
    void testSendLogs_emptyLogProducesNoBatches() {
        LoggingConfiguration config = buildConfig(LogLevel.INFO);

        exporter.sendLogsToCloudLoggingService(config, buildEntry(""));

        assertTrue(exporter.capturedBatches.isEmpty());
    }

    @Test
    void testSendLogs_largeBatchIsSplitWhenOverSizeLimit() {
        LoggingConfiguration config = buildConfig(LogLevel.INFO);
        // Build a log entry whose JSON representation exceeds 3.5 MB
        String largeText = "x".repeat(1024 * 1024);
        String log1 = logLine(LOG_DATE, "INFO", "deploy-app.svc", "[t] " + largeText);
        String log2 = logLine(LOG_DATE, "INFO", "deploy-app.svc", "[t] " + largeText);
        String log3 = logLine(LOG_DATE, "INFO", "deploy-app.svc", "[t] " + largeText);
        String log4 = logLine(LOG_DATE, "INFO", "deploy-app.svc", "[t] " + largeText);

        exporter.sendLogsToCloudLoggingService(config, buildEntry(log1 + log2 + log3 + log4));

        assertTrue(exporter.capturedBatches.size() > 1);
        assertEquals(4, exporter.capturedEntries()
                                .size());
    }

    // --- failSafe behavior ---

    @Test
    void testSendLogs_failSafeTrue_doesNotThrowOnHttpError() {
        LoggingConfiguration config = buildConfig(LogLevel.INFO, true);
        exporter.responseStatus = HttpStatus.INTERNAL_SERVER_ERROR;

        exporter.sendLogsToCloudLoggingService(config, INFO_LOG);
        // no exception
    }

    @Test
    void testSendLogs_failSafeFalse_throwsOnHttpError() {
        LoggingConfiguration config = buildConfig(LogLevel.INFO, false);
        exporter.responseStatus = HttpStatus.INTERNAL_SERVER_ERROR;

        assertThrows(SLException.class, () -> exporter.sendLogsToCloudLoggingService(config, INFO_LOG));
    }

    @Test
    void testSendLogs_nullResponseDoesNotThrow() {
        LoggingConfiguration config = buildConfig(LogLevel.INFO, false);
        exporter.returnNullResponse = true;

        exporter.sendLogsToCloudLoggingService(config, INFO_LOG);
        // null response is treated as success
    }

    @Test
    void testGetUnsendProcessLogs_returnsLogsFromService() throws FileStorageException {
        LoggingConfiguration config = buildConfig(LogLevel.INFO);
        OperationLogEntry entry = buildEntry(INFO_LOG);
        when(processLogsPersistenceService.listOperationLogsBySpaceAndOperationId(SPACE_ID, OPERATION_ID)).thenReturn(List.of(entry));

        List<OperationLogEntry> result = exporter.getUnsendProcessLogs(config);

        assertEquals(1, result.size());
        assertEquals(entry, result.get(0));
    }

    @Test
    void testGetUnsendProcessLogs_failSafeTrue_returnsEmptyListOnStorageException() throws FileStorageException {
        LoggingConfiguration config = buildConfig(LogLevel.INFO, true);
        when(processLogsPersistenceService.listOperationLogsBySpaceAndOperationId(anyString(),
                                                                                  anyString())).thenThrow(
            new FileStorageException("db error"));

        List<OperationLogEntry> result = exporter.getUnsendProcessLogs(config);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetUnsendProcessLogs_failSafeFalse_throwsOnStorageException() throws FileStorageException {
        LoggingConfiguration config = buildConfig(LogLevel.INFO, false);
        when(processLogsPersistenceService.listOperationLogsBySpaceAndOperationId(anyString(),
                                                                                  anyString())).thenThrow(
            new FileStorageException("db error"));

        assertThrows(SLException.class, () -> exporter.getUnsendProcessLogs(config));
    }

    @Test
    void testRemoveClientFromCache_newClientCreatedOnNextSend() {
        LoggingConfiguration config = buildConfig(LogLevel.INFO);
        exporter.sendLogsToCloudLoggingService(config, INFO_LOG);
        int clientCreationsAfterFirst = exporter.clientCreations;

        exporter.removeClientFromCache(OPERATION_ID);
        exporter.sendLogsToCloudLoggingService(config, INFO_LOG);

        assertEquals(clientCreationsAfterFirst + 1, exporter.clientCreations);
    }

    @Test
    void testSendLogs_cachedClientReusedOnSubsequentCalls() {
        LoggingConfiguration config = buildConfig(LogLevel.INFO);
        exporter.sendLogsToCloudLoggingService(config, INFO_LOG);
        int clientCreationsAfterFirst = exporter.clientCreations;

        exporter.sendLogsToCloudLoggingService(config, INFO_LOG);

        assertEquals(clientCreationsAfterFirst, exporter.clientCreations);
    }

    private static String logLine(String date, String level, String logName, String text) {
        return "#" + date + "#org.example.Logger#" + level + "#" + logName + "#main#\n" + text + "\n";
    }

    private static LoggingConfiguration buildConfig(LogLevel logLevel) {
        return buildConfig(logLevel, true);
    }

    private static LoggingConfiguration buildConfig(LogLevel logLevel, boolean failSafe) {
        return ImmutableLoggingConfiguration.builder()
                                            .operationId(OPERATION_ID)
                                            .mtaSpaceId(SPACE_ID)
                                            .logLevel(logLevel)
                                            .isFailSafe(failSafe)
                                            .endpointUrl("https://cls.example.com")
                                            .serverCa("server-ca")
                                            .clientCert("client-cert")
                                            .clientKey("client-key")
                                            .build();
    }

    private static OperationLogEntry buildEntry(String log) {
        return ImmutableOperationLogEntry.builder()
                                         .operationId(OPERATION_ID)
                                         .operationLog(log)
                                         .operationLogName("test-log")
                                         .build();
    }

    private class TestOperationLogsExporter extends OperationLogsExporter {

        final List<List<ExternalOperationLogEntry>> capturedBatches = new ArrayList<>();
        HttpStatus responseStatus = HttpStatus.OK;
        boolean returnNullResponse = false;
        int clientCreations = 0;

        TestOperationLogsExporter(ProcessLogsPersistenceService processLogsPersistenceService) {
            super(processLogsPersistenceService);
        }

        List<ExternalOperationLogEntry> capturedEntries() {
            return capturedBatches.stream()
                                  .flatMap(List::stream)
                                  .toList();
        }

        @Override
        @SuppressWarnings("unchecked")
        protected WebClient createWebClientWithMtls(LoggingConfiguration loggingConfiguration) {
            clientCreations++;
            WebClient webClient = mock(WebClient.class);
            WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
            WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
            WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

            when(webClient.post()).thenReturn(uriSpec);
            when(uriSpec.header(anyString(), anyString())).thenReturn(uriSpec);
            when(uriSpec.bodyValue(any())).thenAnswer(invocation -> {
                String json = invocation.getArgument(0);
                List<ExternalOperationLogEntry> entries = JsonUtil.convertJsonToList(json,
                                                                                     new TypeReference<List<ExternalOperationLogEntry>>() {
                                                                                     });
                capturedBatches.add(entries);
                return headersSpec;
            });
            when(headersSpec.retrieve()).thenReturn(responseSpec);

            if (returnNullResponse) {
                when(responseSpec.toBodilessEntity()).thenReturn(Mono.empty());
            } else {
                ResponseEntity<Void> response = ResponseEntity.status(responseStatus)
                                                              .build();
                when(responseSpec.toBodilessEntity()).thenReturn(Mono.just(response));
            }

            return webClient;
        }
    }
}
