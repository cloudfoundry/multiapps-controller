package org.cloudfoundry.multiapps.controller.persistence.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableLoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.LogLevel;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
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

    // --- extractLogName ---

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

    // --- getLogsFromOperationLogEntry: happy path ---

    @Test
    void getLogsFromOperationLogEntry_singleInfoLine_groupedByLevel() {
        String input = logLine(DATE, "INFO", "deploy-app.svc", "[main] hello");

        Map<LogLevel, List<OperationLogsExporter.OperationLog>> result = converter.getLogsFromOperationLogEntry(buildConfig(true),
                                                                                                                       input);

        List<OperationLogsExporter.OperationLog> infos = result.get(LogLevel.INFO);
        assertEquals(1, infos.size());
        // NOTE: extractMessage strips one trailing character after .trim() — likely a bug
        // (the -1 was probably meant to handle a trailing newline that .trim() already removes).
        // The test pins current behavior; flag for follow-up.
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

        Map<LogLevel, List<OperationLogsExporter.OperationLog>> result = converter.getLogsFromOperationLogEntry(buildConfig(true),
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

        Map<LogLevel, List<OperationLogsExporter.OperationLog>> result = converter.getLogsFromOperationLogEntry(buildConfig(true),
                                                                                                                       input);

        List<OperationLogsExporter.OperationLog> infos = result.get(LogLevel.INFO);
        assertEquals(2, infos.size());
        // See note in singleInfoLine_groupedByLevel about the trailing-character chop.
        assertEquals("on", infos.get(0)
                                .log());
        assertEquals("tw", infos.get(1)
                                .log());
    }

    // --- getLogsFromOperationLogEntry: edge cases ---

    @Test
    void getLogsFromOperationLogEntry_emptyInput_returnsEmptyMap() {
        Map<LogLevel, List<OperationLogsExporter.OperationLog>> result = converter.getLogsFromOperationLogEntry(buildConfig(true),
                                                                                                                       "");

        assertTrue(result.isEmpty());
    }

    @Test
    void getLogsFromOperationLogEntry_noHeaderLines_returnsEmptyMap() {
        Map<LogLevel, List<OperationLogsExporter.OperationLog>> result = converter.getLogsFromOperationLogEntry(buildConfig(true),
                                                                                                                       "free text without any header\n");

        assertTrue(result.isEmpty());
    }

    @Test
    void getLogsFromOperationLogEntry_unknownLogLevel_throwsNpe() {
        // Production bug: LogLevel.get("FATAL") returns null, and the result EnumMap<LogLevel, …>
        // rejects null keys, so computeIfAbsent(null, …) throws NPE. The map type should either
        // be HashMap or LogLevel.get should fall back to a default; flag for follow-up.
        String input = logLine(DATE, "FATAL", "deploy-app.svc", "[t] unknown level");

        assertThrows(NullPointerException.class, () -> converter.getLogsFromOperationLogEntry(buildConfig(true), input));
    }

    // --- failSafe behavior on malformed input ---
    //
    // The "more messages than levels" branch fires when the split produces more non-blank
    // chunks than the header-line regex matches. Putting body text BEFORE the first header
    // line (so the split's leading chunk has no header to pair with) reliably triggers it.

    @Test
    void getLogsFromOperationLogEntry_moreMessagesThanLevels_failSafeTrue_returnsEmptyMap() {
        String malformed = "orphan body before any header\n"
            + "#" + DATE + "#org.example.Logger#INFO#deploy-app.svc#main#\n"
            + "[t] body\n";

        Map<LogLevel, List<OperationLogsExporter.OperationLog>> result = converter.getLogsFromOperationLogEntry(buildConfig(true),
                                                                                                                       malformed);

        // failSafe=true: util logs and returns; converter then returns Map.of()
        assertTrue(result.isEmpty());
    }

    @Test
    void getLogsFromOperationLogEntry_moreMessagesThanLevels_failSafeFalse_throws() {
        String malformed = "orphan body before any header\n"
            + "#" + DATE + "#org.example.Logger#INFO#deploy-app.svc#main#\n"
            + "[t] body\n";

        assertThrows(SLException.class, () -> converter.getLogsFromOperationLogEntry(buildConfig(false), malformed));
    }

    // --- helpers ---

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
