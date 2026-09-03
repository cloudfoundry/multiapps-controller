package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.CloudException;
import org.cloudfoundry.multiapps.controller.client.facade.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ApplicationLog;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableApplicationLog;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.LogCacheReadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

public class LogCacheClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogCacheClient.class);
    private static final String SOURCE_TYPE_KEY_NAME = "source_type";
    private static final int MAX_LOG_COUNT = 1000;

    private final RestClient restClient;

    public LogCacheClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public List<ApplicationLog> getRecentLogs(UUID applicationGuid, LocalDateTime offset) {
        LOGGER.info(Messages.TRYING_TO_GET_APP_LOGS);
        LogCacheReadResponse response = readApplicationLogs(applicationGuid, offset);

        if (response == null) {
            throw new CloudException(MessageFormat.format(Messages.FAILED_TO_FETCH_APP_LOGS_FOR_APP, applicationGuid));
        }

        LOGGER.info(Messages.APP_LOGS_WERE_FETCHED_SUCCESSFULLY);
        return response.batch()
                       .stream()
                       .map(this::mapToAppLog)
                       // we use a linked list so that the log messages can be a LIFO sequence
                       // that way, we avoid unnecessary sorting and copying to and from another collection/array
                       .collect(LinkedList::new, LinkedList::addFirst, LinkedList::addAll);
    }

    private LogCacheReadResponse readApplicationLogs(UUID applicationGuid, LocalDateTime offset) {
        var instant = offset.toInstant(ZoneOffset.UTC);
        var secondsInNanos = Duration.ofSeconds(instant.getEpochSecond())
                                     .toNanos();
        long startTime = secondsInNanos + instant.getNano() + 1;

        return restClient.get()
                         .uri(uriBuilder -> uriBuilder.path("/api/v1/read/{sourceId}")
                                                      .queryParam("envelope_types", "LOG")
                                                      .queryParam("descending", Boolean.TRUE)
                                                      .queryParam("limit", MAX_LOG_COUNT)
                                                      .queryParam("start_time", startTime)
                                                      .build(applicationGuid.toString()))
                         .retrieve()
                         .body(LogCacheReadResponse.class);
    }

    private ApplicationLog mapToAppLog(LogCacheReadResponse.Envelope envelope) {
        return ImmutableApplicationLog.builder()
                                      .applicationGuid(envelope.sourceId())
                                      .message(decodeLogPayload(envelope.log()
                                                                        .payload()))
                                      .timestamp(fromLogTimestamp(envelope.timestamp()))
                                      .messageType(fromLogMessageType(envelope.log()
                                                                              .type()))
                                      .sourceName(envelope.tags()
                                                          .get(SOURCE_TYPE_KEY_NAME))
                                      .build();
    }

    private String decodeLogPayload(String base64Encoded) {
        var result = Base64.getDecoder()
                           .decode(base64Encoded.getBytes(StandardCharsets.UTF_8));
        return new String(result, StandardCharsets.UTF_8);
    }

    private LocalDateTime fromLogTimestamp(long timestampNanos) {
        Duration duration = Duration.ofNanos(timestampNanos);
        Instant instant = Instant.ofEpochSecond(duration.getSeconds(), duration.getNano());
        return LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
    }

    private ApplicationLog.MessageType fromLogMessageType(String messageType) {
        return "OUT".equals(messageType) ? ApplicationLog.MessageType.STDOUT : ApplicationLog.MessageType.STDERR;
    }
}
