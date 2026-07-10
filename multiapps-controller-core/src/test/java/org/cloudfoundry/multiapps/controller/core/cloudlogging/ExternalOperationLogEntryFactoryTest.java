package org.cloudfoundry.multiapps.controller.core.cloudlogging;

import java.util.List;

import org.cloudfoundry.multiapps.controller.persistence.model.ExternalOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableLoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.LogLevel;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.OperationLogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalOperationLogEntryFactoryTest {

    private static final String OPERATION_ID = "op-123";
    private static final String LOG_DATE = "2024 01 15 10:30:00.000";
    private static final String INFO_LOG = logLine(LOG_DATE, "INFO", "deploy-app.hello-backend", "[main] Starting deployment");
    private static final String WARN_LOG = logLine(LOG_DATE, "WARN", "deploy-app.hello-backend", "[main] Low memory");
    private static final String ERROR_LOG = logLine(LOG_DATE, "ERROR", "deploy-app.hello-backend", "[main] Deployment failed");
    private static final String DEBUG_LOG = logLine(LOG_DATE, "DEBUG", "deploy-app.hello-backend", "[main] Debug info");

    private ExternalOperationLogEntryFactory factory;

    @BeforeEach
    void setUp() {
        factory = new ExternalOperationLogEntryFactory(new CloudLoggingServiceMessageConverter());
    }

    @Test
    void fromMessage_producesOneEntryPerAllowedLine() {
        List<ExternalOperationLogEntry> entries = factory.fromMessage(buildConfig(LogLevel.INFO), INFO_LOG + WARN_LOG);

        assertEquals(2, entries.size());
    }

    @Test
    void fromMessage_filtersOutLinesBelowConfiguredLevel() {
        List<ExternalOperationLogEntry> entries = factory.fromMessage(buildConfig(LogLevel.ERROR), INFO_LOG + DEBUG_LOG + ERROR_LOG);

        assertEquals(1, entries.size());
        assertEquals("ERROR", entries.get(0)
                                     .getLevel());
    }

    @Test
    void fromMessage_setsLogNameFromMessage() {
        List<ExternalOperationLogEntry> entries = factory.fromMessage(buildConfig(LogLevel.INFO), INFO_LOG);

        assertEquals("hello-backend", entries.get(0)
                                             .getOperationLogName());
    }

    @Test
    void fromMessage_setsCorrelationIdFromConfiguration() {
        List<ExternalOperationLogEntry> entries = factory.fromMessage(buildConfig(LogLevel.INFO), INFO_LOG);

        assertEquals(OPERATION_ID, entries.get(0)
                                          .getCorrelationId());
    }

    @Test
    void fromMessage_emptyMessageProducesNoEntries() {
        List<ExternalOperationLogEntry> entries = factory.fromMessage(buildConfig(LogLevel.INFO), "");

        assertTrue(entries.isEmpty());
    }

    @Test
    void fromOperationLogEntry_producesOneEntryPerAllowedLine() {
        OperationLogEntry entry = buildEntry(INFO_LOG + WARN_LOG + ERROR_LOG);

        List<ExternalOperationLogEntry> entries = factory.fromOperationLogEntry(buildConfig(LogLevel.INFO), entry);

        assertEquals(3, entries.size());
    }

    @Test
    void fromOperationLogEntry_filtersOutLinesBelowConfiguredLevel() {
        OperationLogEntry entry = buildEntry(INFO_LOG + DEBUG_LOG);

        List<ExternalOperationLogEntry> entries = factory.fromOperationLogEntry(buildConfig(LogLevel.WARN), entry);

        assertTrue(entries.isEmpty());
    }

    @Test
    void fromOperationLogEntry_setsLogNameFromEntry() {
        OperationLogEntry entry = ImmutableOperationLogEntry.builder()
                                                            .operationId(OPERATION_ID)
                                                            .operationLog(INFO_LOG)
                                                            .operationLogName("my-log")
                                                            .build();

        List<ExternalOperationLogEntry> entries = factory.fromOperationLogEntry(buildConfig(LogLevel.INFO), entry);

        assertEquals("my-log", entries.get(0)
                                      .getOperationLogName());
    }

    @Test
    void fromOperationLogEntry_setsCorrelationIdFromConfiguration() {
        List<ExternalOperationLogEntry> entries = factory.fromOperationLogEntry(buildConfig(LogLevel.INFO), buildEntry(INFO_LOG));

        assertEquals(OPERATION_ID, entries.get(0)
                                          .getCorrelationId());
    }

    @Test
    void fromLevelledMessage_setsProvidedLevel() {
        ExternalOperationLogEntry entry = factory.fromLevelledMessage(buildConfig(LogLevel.INFO), INFO_LOG, LogLevel.WARN);

        assertEquals("WARN", entry.getLevel());
    }

    @Test
    void fromLevelledMessage_setsCorrelationIdAndLogName() {
        ExternalOperationLogEntry entry = factory.fromLevelledMessage(buildConfig(LogLevel.INFO), INFO_LOG, LogLevel.INFO);

        assertEquals(OPERATION_ID, entry.getCorrelationId());
        assertEquals("hello-backend", entry.getOperationLogName());
    }

    @Test
    void fromLevelledMessage_generatesTimestampAndId() {
        ExternalOperationLogEntry entry = factory.fromLevelledMessage(buildConfig(LogLevel.INFO), INFO_LOG, LogLevel.INFO);

        assertNotNull(entry.getId());
        assertNotNull(entry.getTimestamp());
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
}
