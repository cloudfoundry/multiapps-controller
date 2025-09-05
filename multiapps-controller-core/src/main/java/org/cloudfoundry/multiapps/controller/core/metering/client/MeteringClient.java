package org.cloudfoundry.multiapps.controller.core.metering.client;

import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.cloudfoundry.multiapps.controller.client.facade.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.core.metering.configuration.MeteringConfiguration;
import org.cloudfoundry.multiapps.controller.core.metering.model.Credentials;
import org.cloudfoundry.multiapps.controller.core.metering.model.ImmutableConsumer;
import org.cloudfoundry.multiapps.controller.core.metering.model.ImmutableEnvironment;
import org.cloudfoundry.multiapps.controller.core.metering.model.ImmutableUsagePayload;
import org.cloudfoundry.multiapps.controller.core.metering.model.UsagePayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeteringClient {

    private final Credentials credentials;
    private final HttpClient httpClient;
    private final String USAGE_INGESTION_URL_TEMPLATE = "{0}/v2/accounts/{1}/namespaces/metering/datastreams/{2}/eventBatch";
    private final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
    private static final Logger LOGGER = LoggerFactory.getLogger(MeteringConfiguration.class);

    public MeteringClient(Credentials credentials, HttpClient httpClient) {
        this.credentials = credentials;
        this.httpClient = httpClient;
        sendTestUsage();
    }

    public void sendTestUsage() {
        recordUsage("cf-eu10-canary", "b71fc53b-7518-4d2b-b8da-00aaed032e0c", List.of("deploy"), "deploy-started");
    }

    public void recordUsage(String landscape, String organisationId, List<String> customDimensions, String measureMessage) {
        UsagePayload usagePayload = createUsagePayload(landscape, organisationId, customDimensions, measureMessage);
        String url = createUsageIngestionUrl();

        HttpPost httpPost = new HttpPost(URI.create(url));
        LOGGER.error(JsonUtil.convertToJson(List.of(usagePayload)));
        httpPost.setEntity(new StringEntity(JsonUtil.convertToJson(List.of(usagePayload))));
        try {
            CloseableHttpResponse a = (CloseableHttpResponse) httpClient.execute(httpPost);
            LOGGER.error(String.valueOf(a.getCode()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private UsagePayload createUsagePayload(String landscape, String organisationId, List<String> customDimensions, String measureMessage) {
        return ImmutableUsagePayload.builder()
                                    .id(UUID.randomUUID())
                                    .timestamp(ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
                                                            .format(DATE_TIME_FORMAT))
                                    .customDimensions(getCustomDimensions(customDimensions))
                                    .measure(getMeasure(measureMessage))
                                    .consumer(ImmutableConsumer.builder()
                                                               .region(landscape)
                                                               .btp(ImmutableEnvironment.builder()
                                                                                        .subAccount(organisationId)
                                                                                        .build())
                                                               .build())
                                    .build();
    }

    private Map<String, String> getCustomDimensions(List<String> dimensions) {
        Map<String, String> meteringDimensions = new HashMap<>();
        for (int i = 0; i < dimensions.size(); i++) {
            meteringDimensions.put("dimension" + (i + 1), dimensions.get(i));
        }
        return meteringDimensions;
    }

    private Map<String, Object> getMeasure(String message) {
        return Map.of("id", message, "value", 1);
    }

    private String createUsageIngestionUrl() {
        return MessageFormat.format(USAGE_INGESTION_URL_TEMPLATE, credentials.usageIngestionEndpoint(),
                                    credentials.meteringAccount(), credentials.dataStreamName());
    }
}
