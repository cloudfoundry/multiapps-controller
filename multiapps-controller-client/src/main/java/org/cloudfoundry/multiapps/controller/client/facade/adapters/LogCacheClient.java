package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import org.cloudfoundry.multiapps.controller.client.facade.CloudException;
import org.cloudfoundry.multiapps.controller.client.facade.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ApplicationLog;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableApplicationLog;
import org.cloudfoundry.multiapps.controller.client.facade.oauth2.OAuthClient;
import org.cloudfoundry.logcache.v1.Envelope;
import org.cloudfoundry.logcache.v1.EnvelopeType;
import org.cloudfoundry.logcache.v1.ReadRequest;
import org.cloudfoundry.logcache.v1.ReadResponse;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.logcache.v1.ReactorLogCacheClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.Map;
import java.util.UUID;

public class LogCacheClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogCacheClient.class);
    private static final String SOURCE_TYPE_KEY_NAME = "source_type";
    private static final int MAX_LOG_COUNT = 1000;
    private final OAuthClient oAuthClient;
    private final Map<String, String> requestTags;
    private final ConnectionContext connectionContext;

    public LogCacheClient(OAuthClient oAuthClient, Map<String, String> requestTags, ConnectionContext connectionContext) {
        this.oAuthClient = oAuthClient;
        this.requestTags = requestTags;
        this.connectionContext = connectionContext;
    }

    public List<ApplicationLog> getRecentLogs(UUID applicationGuid, LocalDateTime offset) {
        LOGGER.info(Messages.TRYING_TO_GET_APP_LOGS);
        org.cloudfoundry.logcache.v1.LogCacheClient logCacheClient = createReactorLogCacheClient();
        ReadResponse applicationLogsResponse = readApplicationLogs(logCacheClient, applicationGuid, offset);

        if (applicationLogsResponse != null) {
            LOGGER.info(Messages.APP_LOGS_WERE_FETCHED_SUCCESSFULLY);
            return applicationLogsResponse.getEnvelopes()
                                          .getBatch()
                                          .stream()
                                          .map(this::mapToAppLog)
                                          // we use a linked list so that the log messages can be a LIFO sequence
                                          // that way, we avoid unnecessary sorting and copying to and from another collection/array
                                          .collect(LinkedList::new, LinkedList::addFirst, LinkedList::addAll);
        } else {
            throw new CloudException(MessageFormat.format(Messages.FAILED_TO_FETCH_APP_LOGS_FOR_APP, applicationGuid));
        }

    }

    private ReactorLogCacheClient createReactorLogCacheClient() {
        return ReactorLogCacheClient.builder()
                                    .requestTags(requestTags)
                                    .connectionContext(connectionContext)
                                    .tokenProvider(oAuthClient.getTokenProvider())
                                    .build();
    }

    private ReadResponse readApplicationLogs(org.cloudfoundry.logcache.v1.LogCacheClient logCacheClient, UUID applicationGuid,
                                             LocalDateTime offset) {
        var instant = offset.toInstant(ZoneOffset.UTC);
        var secondsInNanos = Duration.ofSeconds(instant.getEpochSecond())
                                     .toNanos();
        return logCacheClient.read(ReadRequest.builder()
                                              .envelopeType(EnvelopeType.LOG)
                                              .sourceId(applicationGuid.toString())
                                              .descending(Boolean.TRUE)
                                              .limit(MAX_LOG_COUNT)
                                              .startTime(secondsInNanos + instant.getNano() + 1)
                                              .build())
                             .block();
    }

    private ApplicationLog mapToAppLog(Envelope envelope) {
        return ImmutableApplicationLog.builder()
                                      .applicationGuid(envelope.getSourceId())
                                      .message(decodeLogPayload(envelope.getLog()
                                                                        .getPayload()))
                                      .timestamp(fromLogTimestamp(envelope.getTimestamp()))
                                      .messageType(fromLogMessageType(envelope.getLog()
                                                                              .getType()
                                                                              .getValue()))
                                      .sourceName(envelope.getTags()
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
