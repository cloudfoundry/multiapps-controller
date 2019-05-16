package com.sap.cloud.lm.sl.cf.core.cf.clients;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.util.UUID;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudEntity.Meta;
import org.cloudfoundry.client.lib.domain.CloudInfo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.cf.client.util.ExecutionRetrier;
import com.sap.cloud.lm.sl.common.util.Tester;
import com.sap.cloud.lm.sl.common.util.Tester.Expectation;

public class RecentLogsRetrieverTest {

    private static final String CONTROLLER_URL = "https://api.cf.sap.hana.ondemand.com";
    private static final String LOGGING_ENDPOINT_URL = "https://api.cf.sap.hana.ondemand.com/log";
    private static final String APP_NAME = "my-app";
    private static final UUID APP_UUID = UUID.randomUUID();

    private final Tester tester = Tester.forClass(getClass());

    @Mock
    private CloudControllerClient client;
    @Mock
    private RestTemplateFactory restTemplateFactory;
    @Mock
    private RestTemplate restTemplate;

    private ExecutionRetrier fastRetrier = new ExecutionRetrier().withRetryCount(1)
        .withWaitTimeBetweenRetriesInMillis(1);

    private RecentLogsRetriever recentLogsRetriever;

    private class RecentLogsRetrieverMock extends RecentLogsRetriever {

        public RecentLogsRetrieverMock(RestTemplateFactory restTemplateFactory) {
            super(restTemplateFactory);
        }

        @Override
        protected ExecutionRetrier getRetrier() {
            return fastRetrier;
        }
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.recentLogsRetriever = new RecentLogsRetrieverMock(restTemplateFactory);
        CloudInfo cloudInfo = Mockito.mock(CloudInfo.class);
        Mockito.when(cloudInfo.getLoggingEndpoint())
            .thenReturn(LOGGING_ENDPOINT_URL);
        Mockito.when(client.getCloudInfo())
            .thenReturn(cloudInfo);
        Mockito.when(client.getApplication(APP_NAME))
            .thenReturn(createDummpyApp());
        Mockito.when(client.getCloudControllerUrl())
            .thenReturn(new URL(CONTROLLER_URL));
        Mockito.when(restTemplateFactory.getRestTemplate(client))
            .thenReturn(restTemplate);
    }

    @Test
    public void testGetRecentLogsWithError() {
        Mockito
            .when(restTemplate.exchange(LOGGING_ENDPOINT_URL + RecentLogsRetriever.RECENT_LOGS_ENDPOINT, HttpMethod.GET, null,
                Resource.class, APP_UUID))
            .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Something fails"));

        tester.test(() -> recentLogsRetriever.getRecentLogs(client, APP_NAME),
            new Expectation(Expectation.Type.EXCEPTION, "500 Something fails"));
    }

    @Test
    public void testGetRecentLogsWithErrorFailSafe() {
        fastRetrier = fastRetrier.failSafe();
        Mockito
            .when(restTemplate.exchange(LOGGING_ENDPOINT_URL + RecentLogsRetriever.RECENT_LOGS_ENDPOINT, HttpMethod.GET, null,
                Resource.class, APP_UUID))
            .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Something fails"));
        assertEquals(null, recentLogsRetriever.getRecentLogs(client, APP_NAME));
    }

    private CloudApplication createDummpyApp() {
        Meta meta = new Meta(APP_UUID, null, null);
        return new CloudApplication(meta, APP_NAME);
    }

}
