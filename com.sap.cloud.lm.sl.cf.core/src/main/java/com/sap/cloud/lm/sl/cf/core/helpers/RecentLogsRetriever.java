package com.sap.cloud.lm.sl.cf.core.helpers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.fileupload.MultipartStream;
import org.apache.commons.fileupload.MultipartStream.MalformedStreamException;
import org.apache.commons.io.IOUtils;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.google.protobuf.InvalidProtocolBufferException;
import com.sap.cloud.lm.sl.cf.client.events.EventFactory;
import com.sap.cloud.lm.sl.cf.client.events.LogFactory.LogMessage;
import com.sap.cloud.lm.sl.cf.client.events.LogFactory.LogMessage.MessageType;
import com.sap.cloud.lm.sl.cf.client.util.StreamUtil;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

public class RecentLogsRetriever {

    @Inject
    protected RestTemplateFactory restTemplateFactory;

    public List<ApplicationLog> getRecentLogs(CloudFoundryOperations client, String appName) {
        UUID applicationGuid = client.getApplication(appName).getMeta().getGuid();
        String dopplerEndpoint = getDopplerEndpoint(client.getCloudInfo().getLoggregatorEndpoint());

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

    private RestTemplate getRestTemplate(CloudFoundryOperations client) {
        return restTemplateFactory.getRestTemplate(client);
    }

    private List<ApplicationLog> convertToApplicationLogs(List<LogMessageConverter> logMessages) {
        return logMessages.stream().map(message -> message.convertToApplicationLog()).collect(Collectors.toList());
    }

    private List<LogMessageConverter> extractLogMessages(ResponseEntity<Resource> responseResource)
        throws MalformedStreamException, InvalidProtocolBufferException, IOException {
        List<LogMessageConverter> parsedLogs = new ArrayList<>();
        MediaType contentType = responseResource.getHeaders().getContentType();
        String boundary = contentType.getParameter("boundary");
        InputStream responseInputStream = responseResource.getBody().getInputStream();
        try {
            MultipartStream multipartStream = new MultipartStream(responseInputStream, boundary.getBytes(StandardCharsets.UTF_8), 16 * 1024,
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
        EventFactory.Envelope logEnvelope = EventFactory.Envelope.parseFrom(StreamUtil.removeLeadingLine(part.toByteArray()));
        if (logEnvelope.hasLogMessage()) {
            parsedLogs.add(new LogMessageConverter(logEnvelope.getLogMessage()));
        }
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
            return new ApplicationLog(logMessage.getAppId(), logMessage.getMessage().toStringUtf8(), new Date(logMessage.getTimestamp()),
                getMessageType(), logMessage.getSourceType(), logMessage.getSourceInstance());
        }

        private org.cloudfoundry.client.lib.domain.ApplicationLog.MessageType getMessageType() {
            if (logMessage.getMessageType() == MessageType.ERR) {
                return ApplicationLog.MessageType.STDERR;
            }

            return ApplicationLog.MessageType.STDOUT;
        }
    }

}
