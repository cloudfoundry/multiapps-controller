package org.cloudfoundry.multiapps.controller.persistence.model;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public enum LogLevel {

    INFO, WARN, DEBUG, ERROR, TRACE;

    private static final Map<LogLevel, List<LogLevel>> logLevelLoggingType = setupLogLevels();

    public static LogLevel get(String value) {
        for (LogLevel logLevel : values()) {
            if (logLevel.name()
                        .equals(value)) {
                return logLevel;
            }
        }
        return null;
    }

    public static Map<LogLevel, List<LogLevel>> getLogLevelLoggingType() {
        return logLevelLoggingType;
    }

    private static Map<LogLevel, List<LogLevel>> setupLogLevels() {
        Map<LogLevel, List<LogLevel>> logLevels = new EnumMap<>(LogLevel.class);
        logLevels.put(TRACE, List.of(TRACE, DEBUG, INFO, WARN, ERROR));
        logLevels.put(DEBUG, List.of(DEBUG, INFO, WARN, ERROR));
        logLevels.put(INFO, List.of(INFO, WARN, ERROR));
        logLevels.put(WARN, List.of(WARN, ERROR));
        logLevels.put(ERROR, List.of(ERROR));
        return logLevels;
    }

    public static boolean isValid(String logLevel) {
        return Arrays.stream(values())
                     .anyMatch(value -> value.name()
                                             .equals(logLevel));
    }
}
