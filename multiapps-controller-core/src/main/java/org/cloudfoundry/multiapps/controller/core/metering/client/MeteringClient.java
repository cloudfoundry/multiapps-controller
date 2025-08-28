package org.cloudfoundry.multiapps.controller.core.metering.client;

import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Map;

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
    //private final String USAGE_INGESTION_URL_TEMPLATE = "{0}.{1}/v2/accounts/{2}/namespaces/metering/datastreams/{3}/eventBatch";
    private final String USAGE_INGESTION_URL_TEMPLATE = "{0}/v2/accounts/{1}/namespaces/metering/datastreams/{2}/eventBatch";
    private static final Logger LOGGER = LoggerFactory.getLogger(MeteringConfiguration.class);

    public MeteringClient(Credentials credentials, HttpClient httpClient) {
        this.credentials = credentials;
        this.httpClient = httpClient;
        sendTestUsage();
    }

    public void sendTestUsage() {
        recordUsage(ImmutableUsagePayload.builder()
                                         .consumer(ImmutableConsumer.builder()
                                                                    .region("eu10-canary")
                                                                    .btp(ImmutableEnvironment.builder()
                                                                                             .subAccount(
                                                                                                 "b71fc53b-7518-4d2b-b8da-00aaed032e0c")
                                                                                             .build())
                                                                    .build())
                                         .customDimensions(Map.of("test", "test"))
                                         .build());
    }

    public void recordUsage(UsagePayload usagePayload) {
        String url = createUsageIngestionUrl();

        HttpPost httpPost = new HttpPost(URI.create(url));
        LOGGER.error(JsonUtil.convertToJson(usagePayload));
        httpPost.setEntity(new StringEntity(JsonUtil.convertToJson(usagePayload)));
        try {
            CloseableHttpResponse a = (CloseableHttpResponse) httpClient.execute(httpPost);
            LOGGER.error(a.getReasonPhrase());
            LOGGER.error(String.valueOf(a.getCode()));
            LOGGER.error(a.getEntity()
                          .toString());
            LOGGER.error(a.getEntity()
                          .getContent()
                          .toString());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String createUsageIngestionUrl() {
        return MessageFormat.format(USAGE_INGESTION_URL_TEMPLATE, credentials.usageIngestionEndpoint(),
                                    credentials.meteringAccount(), credentials.dataStreamName());
    }
}
