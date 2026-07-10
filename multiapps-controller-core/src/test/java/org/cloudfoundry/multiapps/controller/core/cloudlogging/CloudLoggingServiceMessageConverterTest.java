package org.cloudfoundry.multiapps.controller.core.cloudlogging;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableLoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.LogLevel;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.OperationLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloudLoggingServiceMessageConverterTest {

    private static final String DATE = "2024 01 15 10:30:00.000";
    private static final LocalDateTime EXPECTED_DATE = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

    private CloudLoggingServiceMessageConverter converter;

    @BeforeEach
    void setUp() {
        converter = new CloudLoggingServiceMessageConverter();
    }

    @Test
    void extractLogName_extractsSuffixAfterFirstDot() {
        Optional<String> result = converter.extractLogName(logLine(DATE, "INFO", "deploy-app.hello-backend", "[main] msg"));

        assertEquals(Optional.of("hello-backend"), result);
    }

    @Test
    void extractLogName_returnsEverythingAfterFirstDotWhenMultiple() {
        Optional<String> result = converter.extractLogName(logLine(DATE, "INFO", "a.b.c", "[main] msg"));

        assertEquals(Optional.of("b.c"), result);
    }

    @Test
    void extractLogName_returnsEmptyWhenPatternDoesNotMatch() {
        Optional<String> result = converter.extractLogName("not a log line\n");

        assertEquals(Optional.empty(), result);
    }

    @Test
    void extractLogName_returnsEmptyForEmptyString() {
        Optional<String> result = converter.extractLogName("");

        assertEquals(Optional.empty(), result);
    }

    @Test
    void getLogsFromOperationLogEntry_singleInfoLine_groupedByLevel() {
        String input = logLine(DATE, "INFO", "deploy-app.svc", "[main] hello");

        Map<LogLevel, List<OperationLog>> result = converter.getLogsFromOperationLogEntry(buildConfig(true),
                                                                                          input);

        List<OperationLog> infos = result.get(LogLevel.INFO);
        assertEquals(1, infos.size());
        assertEquals("hell", infos.get(0)
                                  .log());
        assertEquals(EXPECTED_DATE, infos.get(0)
                                         .dateTime());
    }

    @Test
    void getLogsFromOperationLogEntry_multipleLevels_groupedSeparately() {
        String input = logLine(DATE, "INFO", "deploy-app.svc", "[t] i")
            + logLine(DATE, "WARN", "deploy-app.svc", "[t] w")
            + logLine(DATE, "ERROR", "deploy-app.svc", "[t] e");

        Map<LogLevel, List<OperationLog>> result = converter.getLogsFromOperationLogEntry(buildConfig(true),
                                                                                          input);

        assertEquals(1, result.get(LogLevel.INFO)
                              .size());
        assertEquals(1, result.get(LogLevel.WARN)
                              .size());
        assertEquals(1, result.get(LogLevel.ERROR)
                              .size());
    }

    @Test
    void getLogsFromOperationLogEntry_sameLevelMultipleEntries_appendedToList() {
        String input = logLine(DATE, "INFO", "deploy-app.svc", "[t] one")
            + logLine(DATE, "INFO", "deploy-app.svc", "[t] two");

        Map<LogLevel, List<OperationLog>> result = converter.getLogsFromOperationLogEntry(buildConfig(true),
                                                                                          input);

        List<OperationLog> infos = result.get(LogLevel.INFO);
        assertEquals(2, infos.size());
        assertEquals("on", infos.get(0)
                                .log());
        assertEquals("tw", infos.get(1)
                                .log());
    }

    @Test
    void getLogsFromOperationLogEntry_emptyInput_returnsEmptyMap() {
        Map<LogLevel, List<OperationLog>> result = converter.getLogsFromOperationLogEntry(buildConfig(true),
                                                                                          "");

        assertTrue(result.isEmpty());
    }

    @Test
    void getLogsFromOperationLogEntry_noHeaderLines_returnsEmptyMap() {
        Map<LogLevel, List<OperationLog>> result = converter.getLogsFromOperationLogEntry(buildConfig(true),
                                                                                          "free text without any header\n");

        assertTrue(result.isEmpty());
    }

    @Test
    void getLogsFromOperationLogEntry_unknownLogLevel_throwsIllegalArgument() {
        String input = logLine(DATE, "FATAL", "deploy-app.svc", "[t] unknown level");

        assertThrows(IllegalArgumentException.class, () -> converter.getLogsFromOperationLogEntry(buildConfig(true), input));
    }

    @Test
    void getLogsFromOperationLogEntry_moreMessagesThanLevels_failSafeTrue_returnsEmptyMap() {
        String malformed = "orphan body before any header\n"
            + "#" + DATE + "#org.example.Logger#INFO#deploy-app.svc#main#\n"
            + "[t] body\n";

        Map<LogLevel, List<OperationLog>> result = converter.getLogsFromOperationLogEntry(buildConfig(true),
                                                                                          malformed);

        assertTrue(result.isEmpty());
    }

    @Test
    void getLogsFromOperationLogEntry_moreMessagesThanLevels_failSafeFalse_throws() {
        String malformed = "orphan body before any header\n"
            + "#" + DATE + "#org.example.Logger#INFO#deploy-app.svc#main#\n"
            + "[t] body\n";

        assertThrows(SLException.class, () -> converter.getLogsFromOperationLogEntry(buildConfig(false), malformed));
    }

    private static String logLine(String date, String level, String logName, String text) {
        return "#" + date + "#org.example.Logger#" + level + "#" + logName + "#main#\n" + text + "\n";
    }

    private static LoggingConfiguration buildConfig(boolean failSafe) {
        return ImmutableLoggingConfiguration.builder()
                                            .operationId("op")
                                            .mtaSpaceId("space")
                                            .logLevel(LogLevel.INFO)
                                            .isFailSafe(failSafe)
                                            .endpointUrl("https://cls.example.com")
                                            .serverCa("ca")
                                            .clientCert("cert")
                                            .clientKey("key")
                                            .build();
    }
}
