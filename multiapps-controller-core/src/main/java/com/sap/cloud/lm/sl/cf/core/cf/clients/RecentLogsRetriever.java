package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.fileupload.MultipartStream;
import org.apache.commons.io.IOUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.google.protobuf.InvalidProtocolBufferException;
import com.sap.cloud.lm.sl.cf.client.events.EventFactory;
import com.sap.cloud.lm.sl.cf.client.events.LogFactory.LogMessage;
import com.sap.cloud.lm.sl.cf.client.events.LogFactory.LogMessage.MessageType;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Named
@Profile("cf")
public class RecentLogsRetriever extends CustomControllerClient {

    private final LoggingEndpointGetter loggingEndpointGetter;

    @Inject
    public RecentLogsRetriever(RestTemplateFactory restTemplateFactory, LoggingEndpointGetter loggingEndpointGetter) {
        super(restTemplateFactory);
        this.loggingEndpointGetter = loggingEndpointGetter;
    }

    public List<ApplicationLog> getRecentLogs(CloudControllerClient client, String appName) {
        return new CustomControllerClientErrorHandler().handleErrorsOrReturnResult(() -> attemptToGetRecentLogs(client, appName));
    }

    private List<ApplicationLog> attemptToGetRecentLogs(CloudControllerClient client, String appName) {
        UUID applicationGuid = client.getApplication(appName)
                                     .getMeta()
                                     .getGuid();
        String dopplerEndpoint = getDopplerEndpoint(loggingEndpointGetter.getLoggingEndpoint(client));

        String recentLogsUrl = dopplerEndpoint + "/apps/{guid}/recentlogs";
        ResponseEntity<Resource> responseResource = getRestTemplate(client).exchange(recentLogsUrl, HttpMethod.GET, null, Resource.class,
                                                                                     applicationGuid);
        List<LogMessageConverter> logMessages = null;
        try {
            logMessages = extractLogMessages(responseResource);

        } catch (InvalidProtocolBufferException e) {
            throw new SLException(e, Messages.ERROR_READING_PROTOCOL_BUFFER_LOGS);
        } catch (IOException e) {
            throw new SLException(e, Messages.ERROR_RETRIEVING_RECENT_LOGS);
        }
        return convertToApplicationLogs(logMessages);
    }

    private List<ApplicationLog> convertToApplicationLogs(List<LogMessageConverter> logMessages) {
        return logMessages.stream()
                          .map(LogMessageConverter::convertToApplicationLog)
                          .collect(Collectors.toList());
    }

    private List<LogMessageConverter> extractLogMessages(ResponseEntity<Resource> responseResource) throws IOException {
        List<LogMessageConverter> parsedLogs = new ArrayList<>();
        MediaType contentType = responseResource.getHeaders()
                                                .getContentType();
        String boundary = contentType.getParameter("boundary");
        InputStream responseInputStream = responseResource.getBody()
                                                          .getInputStream();
        try {
            MultipartStream multipartStream = new MultipartStream(responseInputStream,
                                                                  boundary.getBytes(StandardCharsets.UTF_8),
                                                                  16 * 1024,
                                                                  null);
            while (multipartStream.skipPreamble()) {
                ByteArrayOutputStream part = null;
                try {
                    part = new ByteArrayOutputStream();
                    multipartStream.readBodyData(part);
                    parseProtoBufPart(parsedLogs, part);
                } finally {
                    IOUtils.closeQuietly(part);
                }
            }
        } finally {
            IOUtils.closeQuietly(responseInputStream);
        }
        return parsedLogs;
    }

    private void parseProtoBufPart(List<LogMessageConverter> parsedLogs, ByteArrayOutputStream part) throws InvalidProtocolBufferException {
        EventFactory.Envelope logEnvelope = EventFactory.Envelope.parseFrom(removeLeadingNewLine(part.toByteArray()));
        if (logEnvelope.hasLogMessage()) {
            parsedLogs.add(new LogMessageConverter(logEnvelope.getLogMessage()));
        }
    }

    private byte[] removeLeadingNewLine(byte[] data) {
        if (data == null || data.length == 0) {
            return data;
        }
        int from = 0;
        if (data[0] == '\n') {
            from = 1;
        }
        if (data[0] == '\r' && data[1] == '\n') {
            from = 2;
        }
        return Arrays.copyOfRange(data, from, data.length);
    }

    private String getDopplerEndpoint(String loggregatorEndpoint) {
        loggregatorEndpoint = loggregatorEndpoint.replaceFirst("ws", "http");
        return loggregatorEndpoint.replaceFirst("loggregator", "doppler");
    }

    private class LogMessageConverter {
        private LogMessage logMessage;

        public LogMessageConverter(LogMessage logMessage) {
            this.logMessage = logMessage;
        }

        public ApplicationLog convertToApplicationLog() {
            return new ApplicationLog(logMessage.getAppId(),
                                      logMessage.getMessage()
                                                .toStringUtf8(),
                                      new Date(logMessage.getTimestamp()),
                                      getMessageType(),
                                      logMessage.getSourceType(),
                                      logMessage.getSourceInstance());
        }

        private org.cloudfoundry.client.lib.domain.ApplicationLog.MessageType getMessageType() {
            if (logMessage.getMessageType() == MessageType.ERR) {
                return ApplicationLog.MessageType.STDERR;
            }

            return ApplicationLog.MessageType.STDOUT;
        }
    }

}
