package org.cloudfoundry.multiapps.controller.core.cloudlogging;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.model.LogLevel;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.OperationLog;
import org.cloudfoundry.multiapps.controller.persistence.util.CloudLoggingServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named("cloudLoggingServiceMessageConverter")
public class CloudLoggingServiceMessageConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudLoggingServiceMessageConverter.class);
    // SAP Cloud Logging ingest endpoint accepts payloads up to ~4 MB; 3.5 MB leaves headroom for JSON envelope and HTTP framing.
    private static final Pattern MESSAGE_LOG_DATE_PATTERN = Pattern.compile("^#([^#\\r\\n]*)#", Pattern.MULTILINE);
    private static final Pattern MESSAGE_LOG_LEVEL_PATTERN = Pattern.compile("^#[^#\\r\\n]*#[^#\\r\\n]*#([^#\\r\\n]*)#", Pattern.MULTILINE);
    private static final Pattern MESSAGE_LOG_NAME = Pattern.compile("^#[^#\\r\\n]*#[^#\\r\\n]*#[^#\\r\\n]*#([^#\\r\\n]*)#",
                                                                    Pattern.MULTILINE);
    private static final String MESSAGE_SPLITTING_REGEX = "(?m)^#[^#\\r\\n]*#[^#\\r\\n]*#[^#\\r\\n]*#[^#\\r\\n]*#[^#\\r\\n]*#(?:\\r?\\n)?";
    private static final String LOG_NAME_SUFFIX = ".log";

    public Optional<String> extractLogName(String message) {
        Matcher matcher = MESSAGE_LOG_NAME.matcher(message);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String raw = matcher.group(1);
        raw += LOG_NAME_SUFFIX;
        return Optional.of(raw);
    }

    public Map<LogLevel, List<OperationLog>> getLogsFromOperationLogEntry(
        LoggingConfiguration loggingConfiguration, String log) {

        List<String> logLevels = getLogLevels(log);
        List<LocalDateTime> logDates = getLogDate(log);
        if (logLevels.isEmpty()) {
            return new EnumMap<>(LogLevel.class);
        }

        List<String> messages = splitNonBlankMessages(log);
        if (!areParallelListsConsistent(messages, logLevels, logDates)) {
            CloudLoggingServiceUtil.logErrorOrThrowExceptionBasedOnFailSafe(loggingConfiguration, LOGGER, Messages.INVALID_LOG_FILE);
            return Map.of();
        }

        return groupByLogLevel(messages, logLevels, logDates);
    }

    private List<String> splitNonBlankMessages(String log) {
        return Arrays.stream(log.split(MESSAGE_SPLITTING_REGEX))
                     .filter(m -> !m.isBlank())
                     .toList();
    }

    private boolean areParallelListsConsistent(List<String> messages, List<String> logLevels, List<LocalDateTime> logDates) {
        return messages.size() <= logLevels.size() && messages.size() <= logDates.size();
    }

    private Map<LogLevel, List<OperationLog>> groupByLogLevel(List<String> messages,
                                                              List<String> logLevels,
                                                              List<LocalDateTime> logDates) {
        Map<LogLevel, List<OperationLog>> result = new EnumMap<>(LogLevel.class);
        for (int i = 0; i < messages.size(); i++) {
            LogLevel level = LogLevel.get(logLevels.get(i));
            OperationLog entry = buildOperationLog(messages.get(i), logDates.get(i));
            result.computeIfAbsent(level, k -> new ArrayList<>())
                  .add(entry);
        }
        return result;
    }

    private OperationLog buildOperationLog(String rawMessage, LocalDateTime date) {
        return new OperationLog(extractMessage(rawMessage), date);
    }

    private List<String> getLogLevels(String log) {
        Matcher matcher = MESSAGE_LOG_LEVEL_PATTERN.matcher(log);
        List<String> logLevels = new ArrayList<>();

        while (matcher.find()) {
            logLevels.add(matcher.group(1));
        }

        return logLevels;
    }

    private List<LocalDateTime> getLogDate(String log) {
        Matcher matcher = MESSAGE_LOG_DATE_PATTERN.matcher(log);
        List<LocalDateTime> logLevels = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy MM dd HH:mm:ss.SSS");

        while (matcher.find()) {
            LocalDateTime dateTime = LocalDateTime.parse(matcher.group(1), formatter);
            logLevels.add(dateTime);
        }

        return logLevels;
    }

    private String extractMessage(String message) {
        String trimmed = message.substring(message.indexOf("]") + 1)
                                .trim();
        if (trimmed.isEmpty()) {
            return message;
        }
        return trimmed.substring(0, trimmed.length() - 1);
    }
}
