package org.cloudfoundry.multiapps.controller.persistence.model;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogLevelTest {

    static Stream<Arguments> testGet_withValidInput() {
        return Stream.of(Arguments.of("INFO", LogLevel.INFO), Arguments.of("WARN", LogLevel.WARN), Arguments.of("DEBUG", LogLevel.DEBUG),
                         Arguments.of("ERROR", LogLevel.ERROR), Arguments.of("TRACE", LogLevel.TRACE));
    }

    @ParameterizedTest
    @MethodSource
    void testGet_withValidInput(String value, LogLevel expected) {
        assertEquals(expected, LogLevel.get(value));
    }

    @Test
    void testGet_returnsNullForUnknownValue() {
        assertNull(LogLevel.get("UNKNOWN"));
    }

    @Test
    void testGet_returnsNullForNull() {
        assertNull(LogLevel.get(null));
    }

    @Test
    void testIsValid_ValidInput() {
        assertTrue(LogLevel.isValid("INFO"));
    }

    @Test
    void testIsValid_InvalidInput() {
        assertFalse(LogLevel.isValid("test"));
    }

    @Test
    void testGet_isCaseSensitive() {
        assertNull(LogLevel.get("info"));
        assertNull(LogLevel.get("Info"));
    }

    static Stream<Arguments> testLogLevelLoggingType() {
        return Stream.of(
            Arguments.of(LogLevel.TRACE, List.of(LogLevel.TRACE, LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR)),
            Arguments.of(LogLevel.DEBUG, List.of(LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR)),
            Arguments.of(LogLevel.INFO, List.of(LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR)),
            Arguments.of(LogLevel.WARN, List.of(LogLevel.WARN, LogLevel.ERROR)),
            Arguments.of(LogLevel.ERROR, List.of(LogLevel.ERROR)));
    }

    @ParameterizedTest
    @MethodSource
    void testLogLevelLoggingType(LogLevel level, List<LogLevel> expectedAllowedLevels) {
        Map<LogLevel, List<LogLevel>> logLevelLoggingType = LogLevel.getLogLevelLoggingType();
        assertEquals(expectedAllowedLevels, logLevelLoggingType.get(level));
    }

    @Test
    void testGetLogLevelLoggingTypeThatContainsAllLevels() {
        Map<LogLevel, List<LogLevel>> logLevelLoggingType = LogLevel.getLogLevelLoggingType();
        assertEquals(LogLevel.values().length, logLevelLoggingType.size());
        for (LogLevel level : LogLevel.values()) {
            assertTrue(logLevelLoggingType.containsKey(level));
        }
    }

    @Test
    void testLogLevelLoggingTypeThatErrorOnlyIncludesError() {
        List<LogLevel> allowedForError = LogLevel.getLogLevelLoggingType()
                                                 .get(LogLevel.ERROR);
        assertEquals(1, allowedForError.size());
        assertTrue(allowedForError.contains(LogLevel.ERROR));
    }

    @Test
    void testLogLevelLoggingTypeThatTraceIncludesAll() {
        List<LogLevel> allowedForTrace = LogLevel.getLogLevelLoggingType()
                                                 .get(LogLevel.TRACE);
        assertEquals(LogLevel.values().length, allowedForTrace.size());
        for (LogLevel level : LogLevel.values()) {
            assertTrue(allowedForTrace.contains(level));
        }
    }
}
