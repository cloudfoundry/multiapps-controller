package org.cloudfoundry.multiapps.controller.core.cloudlogging;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.model.ExternalOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableLoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.LogLevel;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.OperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.util.CloudLoggingServiceUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class OperationLogsExporterTest {

    private static final String OPERATION_ID = "op-123";
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

    private CapturingHttpClient httpClient;
    private OperationLogsExporter exporter;

    @BeforeEach
    void setUp() {
        httpClient = new CapturingHttpClient();
        CloudLoggingServiceMessageConverter messageConverter = new CloudLoggingServiceMessageConverter();
        exporter = new OperationLogsExporter(httpClient,
                                             new ExternalOperationLogEntryFactory(messageConverter),
                                             new ExternalOperationLogEntryBatcher());
    }

    @Test
    void testSendLogs_withNullLoggingConfiguration_doesNothing() {
        exporter.sendLogsToCloudLoggingService(null, buildEntry(INFO_LOG));

        assertTrue(httpClient.capturedEntries()
                             .isEmpty());
    }

    @Test
    void testSendLogs_withOperationLogEntry_sendsExpectedNumberOfEntries() {
        LoggingConfiguration config = buildConfig(LogLevel.INFO);

        exporter.sendLogsToCloudLoggingService(config, buildEntry(INFO_LOG + WARN_LOG));

        assertEquals(2, httpClient.capturedEntries()
                                  .size());
    }

    @Test
    void testSendLogs_withOperationLogEntry_setsLevelOnEntry() {
        LoggingConfiguration config = buildConfig(LogLevel.INFO);

        exporter.sendLogsToCloudLoggingService(config, buildEntry(WARN_LOG));

        assertEquals("WARN", httpClient.capturedEntries()
                                       .get(0)
                                       .getLevel());
    }

    @Test
    void testSendLogs_withOperationLogEntry_setsCorrelationId() {
        LoggingConfiguration config = buildConfig(LogLevel.INFO);

        exporter.sendLogsToCloudLoggingService(config, buildEntry(INFO_LOG));

        assertEquals(OPERATION_ID, httpClient.capturedEntries()
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

        assertEquals("my-log", httpClient.capturedEntries()
                                         .get(0)
                                         .getOperationLogName());
    }

    @Test
    void testSendLogs_withMessageString_appendsLogSuffixToLogName() {
        LoggingConfiguration config = buildConfig(LogLevel.INFO);

        exporter.sendLogsToCloudLoggingService(config, INFO_LOG);

        assertEquals("deploy-app.hello-backend.log", httpClient.capturedEntries()
                                                .get(0)
                                                .getOperationLogName());
    }

    @Test
    void testSendLogs_withMessageString_setsCorrelationId() {
        LoggingConfiguration config = buildConfig(LogLevel.INFO);

        exporter.sendLogsToCloudLoggingService(config, INFO_LOG);

        assertEquals(OPERATION_ID, httpClient.capturedEntries()
                                             .get(0)
                                             .getCorrelationId());
    }

    @Test
    void testSendLogs_withMessageString_setsLevel() {
        LoggingConfiguration config = buildConfig(LogLevel.INFO);

        exporter.sendLogsToCloudLoggingService(config, ERROR_LOG);

        assertEquals("ERROR", httpClient.capturedEntries()
                                        .get(0)
                                        .getLevel());
    }

    @Test
    void testSendLogs_withMessageString_producesNoBatchesWhenAllFilteredOut() {
        LoggingConfiguration config = buildConfig(LogLevel.ERROR);

        exporter.sendLogsToCloudLoggingService(config, INFO_LOG + DEBUG_LOG);

        assertTrue(httpClient.capturedEntries()
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

        assertEquals(expectedCount, httpClient.capturedEntries()
                                              .size());
    }

    @Test
    void testSendLogs_multipleEntriesAreSentInOneBatch() {
        LoggingConfiguration config = buildConfig(LogLevel.INFO);

        exporter.sendLogsToCloudLoggingService(config, buildEntry(INFO_LOG + WARN_LOG + ERROR_LOG));

        assertEquals(1, httpClient.capturedBatches.size());
        assertEquals(3, httpClient.capturedEntries()
                                  .size());
    }

    @Test
    void testSendLogs_emptyLogProducesNoBatches() {
        LoggingConfiguration config = buildConfig(LogLevel.INFO);

        exporter.sendLogsToCloudLoggingService(config, buildEntry(""));

        assertTrue(httpClient.capturedBatches.isEmpty());
    }

    @Test
    void testSendLogs_largeBatchIsSplitWhenOverSizeLimit() {
        LoggingConfiguration config = buildConfig(LogLevel.INFO);
        String largeText = "x".repeat(1024 * 1024);
        String log1 = logLine(LOG_DATE, "INFO", "deploy-app.svc", "[t] " + largeText);
        String log2 = logLine(LOG_DATE, "INFO", "deploy-app.svc", "[t] " + largeText);
        String log3 = logLine(LOG_DATE, "INFO", "deploy-app.svc", "[t] " + largeText);
        String log4 = logLine(LOG_DATE, "INFO", "deploy-app.svc", "[t] " + largeText);

        exporter.sendLogsToCloudLoggingService(config, buildEntry(log1 + log2 + log3 + log4));

        assertTrue(httpClient.capturedBatches.size() > 1);
        assertEquals(4, httpClient.capturedEntries()
                                  .size());
    }

    @Test
    void testSendLogs_failSafeTrue_doesNotThrowOnHttpError() {
        LoggingConfiguration config = buildConfig(LogLevel.INFO, true);
        httpClient.simulateHttpFailure = true;

        assertDoesNotThrow(() -> exporter.sendLogsToCloudLoggingService(config, INFO_LOG));

        assertEquals(1, httpClient.capturedEntries()
                                  .size());
    }

    @Test
    void testSendLogs_failSafeFalse_throwsOnHttpError() {
        LoggingConfiguration config = buildConfig(LogLevel.INFO, false);
        httpClient.simulateHttpFailure = true;

        assertThrows(SLException.class, () -> exporter.sendLogsToCloudLoggingService(config, INFO_LOG));
    }

    @Test
    void testSendLogs_nullResponseDoesNotThrow() {
        LoggingConfiguration config = buildConfig(LogLevel.INFO, true);
        httpClient.simulateNullResponse = true;

        assertDoesNotThrow(() -> exporter.sendLogsToCloudLoggingService(config, INFO_LOG));

        assertEquals(1, httpClient.capturedEntries()
                                  .size());
    }

    @Test
    void testSendLogs_cachedClientReusedOnSubsequentCalls() {
        LoggingConfiguration config = buildConfig(LogLevel.INFO);
        exporter.sendLogsToCloudLoggingService(config, INFO_LOG);
        int clientCreationsAfterFirst = httpClient.clientCreations;

        exporter.sendLogsToCloudLoggingService(config, INFO_LOG);

        assertEquals(clientCreationsAfterFirst, httpClient.clientCreations);
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
                                            .mtaSpaceId("space-1")
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

    private static class CapturingHttpClient extends CloudLoggingServiceHttpClient {

        final List<List<ExternalOperationLogEntry>> capturedBatches = new ArrayList<>();
        int clientCreations = 0;
        boolean simulateHttpFailure = false;
        boolean simulateNullResponse = false;

        List<ExternalOperationLogEntry> capturedEntries() {
            return capturedBatches.stream()
                                  .flatMap(List::stream)
                                  .toList();
        }

        @Override
        public WebClient createWebClientWithMtls(LoggingConfiguration loggingConfiguration) {
            clientCreations++;
            return mock(WebClient.class);
        }

        @Override
        public void sendLogsToCloudLoggingService(LoggingConfiguration loggingConfiguration, WebClient webClient,
                                                  List<ExternalOperationLogEntry> logEntryBatch) {
            capturedBatches.add(new ArrayList<>(logEntryBatch));
            if (simulateHttpFailure || simulateNullResponse) {
                // The real client treats both an error status and a null response as a failure
                // and routes through CloudLoggingServiceUtil — reproduce that here so the
                // failSafe semantics under test still apply.
                CloudLoggingServiceUtil.logErrorOrThrowExceptionBasedOnFailSafe(loggingConfiguration,
                                                                                LoggerFactory.getLogger(CapturingHttpClient.class),
                                                                                Messages.FAILED_TO_SEND_LOG_MESSAGE_TO_CLS);
            }
        }
    }
}
